/**
 * 功能介绍:
 * 生成对应的MybatisMapper和sql文件
 * 当传递的参数为目录时,会遍历指定目录上所有类文件(groovy或java)并生成
 * 当传递的参数为文件时,根据指定的文件生成
 * 当没有传递任何参数时,会尝试遍历当前目录下的model文件夹并生成
 *
 *
 * 使用示例:
 * 指定文件: groovy EntityGen.groovy /path/to/model/User.java
 * 指定目录: groovy EntityGen.groovy /path/to/model/
 * 默认的model目录: groovy EntityGen.groovy
 *
 *
 * 使用手册:
 * sql中的类型和注释要自己修改, model中的注释会丢失
 *
 *
 * 使用环境:
 * 命令行: brew install groovy && groovy xxx.groovy;
 * IDEA: maven project右键菜单 -> Add Framework Support... -> groovy, 这样就可以在project中对groovy文件有智能提示, 并可以进行debug/run等
 *
 * 注意事项:
 * 因为groovy中数组的字面量用[1,2], 而不是java中的{1,2}, 所以@EqualsAndHashCode(of={"buyerId", "skuId"}) => @EqualsAndHashCode(of=["buyerId", "skuId"])
 */

@Grapes([
        @Grab("com.google.guava:guava:18.0"),
        @Grab(group='org.projectlombok', module='lombok', version='1.16.10'),
        @Grab(group='com.fasterxml.jackson.core', module='jackson-databind', version='2.8.0'),
        @Grab(group='com.fasterxml.jackson.core', module='jackson-core', version='2.8.0'),
        @Grab(group='com.fasterxml.jackson.core', module='jackson-annotations', version='2.8.0'),
        @Grab(group='com.fasterxml.jackson.datatype', module='jackson-datatype-guava', version='2.8.0'),
])

import com.google.common.base.CaseFormat
import java.lang.reflect.Field
import java.lang.reflect.Modifier

import java.nio.file.Files


def path="model" //指定的实体类路径名, 默认为model

println(args)
if(args.any()){
    path=args[0]
}

/**
 * main程序:
 * 遍历指定目录上所有类文件, 或者根据指定的文件, 生成对就的MybatisMapper和sql文件
 */
def gcl = new GroovyClassLoader()
File file = new File(path)

if(file.isFile()){ //如果指定的是文件

    println(file.name)
    Class modelClass = gcl.parseClass(transformJavaFile(file))
    new EntityGenerator().generate(modelClass)

}else { //如果指定的是目录

    file.listFiles().each {
        println(it.name)
        Class modelClass = gcl.parseClass(transformJavaFile(it))
        new EntityGenerator().generate(modelClass)
    }
}

String transformJavaFile(File file){

    List<String> validated = new ArrayList<>()
    List<String> lines = Files.readAllLines(file.toPath())
    for(String line : lines){
        String replaced = line
        if(line.trim().startsWith("@")){
            replaced = line.replace("{", "[").replace("}", "]")
        }
        validated.add(replaced)
    }
    return validated.join("\n")
}


/**
 * Mapper和sql生成类
 */
class EntityGenerator {

    /**
     * 指定数据库表的前缀
     */
    String db="parana";
    /**
     * 这些字段的含义见下文map和sql的模板
     */
    String className="";
    String resultMap="";
    String properties="";
    String tableName="";
    String columns="";
    String values="";
    String sqlFields="";
    String updates="";
    String criteria="";

    /**
     * 要去除的属性名称
     */
    def excluded=["id","updatedAt","createdAt", "metaClass"]

    /**
     * 指定映射关系
     */
    static Map<String,String> map=new HashMap<>();
    static{
        map.put("Long", "BIGINT");
        map.put("Integer", "INT");
        map.put("String", "VARCHAR(64)")
        map.put("Date", "DATETIME")
        map.put("Boolean", "TINYINT")
        map.put("int", "INT")
        map.put("long", "BIGINT")
    }

    void generate(String clazzName){
        try{
            Class<?> clazz=Class.forName(clazzName);
            generate(clazz);
        }catch (Exception ex){
            println ex.getMessage()
        }
    }


    void generate(Class<?> clazz){
        try{
            Field[] fields=clazz.getDeclaredFields();
            def fieldNames=[];

            for(Field field: fields){
                /**
                 * 去除静态属性
                 */
                if(Modifier.isStatic(field.getModifiers())){
                    continue;
                }
                /**
                 * 去除列表中指定的属性
                 */
                if(excluded.contains(field.getName())){
                    continue;
                }

                /**
                 * 获取字段的类型
                 */
                String fieldType=field.getType().getSimpleName();

                /**
                 * 如果该类型不在映射列表中,则不应该生成对应的数据库字段
                 */
                fieldType=map.get(fieldType,null)
                if(fieldType==null){
                    continue;
                }
                String fieldName=camel2Underscore(field.getName());
                sqlFields+="""\t\t`$fieldName`  $fieldType  NULL  COMMENT '',\n"""
            }

            for(Field field : fields){
                if(Modifier.isStatic(field.getModifiers())){
                    continue;
                }
                if(excluded.contains(field.getName())){
                    continue;
                }
                fieldNames.add(field.getName());
            }
            className=clazz.simpleName;
            resultMap=className+"Map";

            tableName=camel2Underscore(className)
            tableName=db+"_"+tableName+"s"

            int i=1;
            for(def field: fieldNames){
                def underscore=camel2Underscore(field)

                values += """#{$field},"""
                columns += """`$underscore`,"""
                properties += """<result column="$underscore" property="$field"/>\n\t\t"""
                updates += """<if test="$field != null"> `$underscore` = #{$field}, </if>\n\t\t\t"""
                criteria += """<if test="$field != null"> and `$underscore` = #{$field}</if>\n\t\t"""

                if(i%5==0){
                    values+="\n\t\t"
                    columns+="\n\t\t"
                }
                ++i;
            }

            new File(className+"Mapper.xml").write("""
<?xml version="1.0" encoding="UTF-8" ?>

<!DOCTYPE mapper PUBLIC "-//mybatis.org//DTD Mapper 3.0//EN" "http://mybatis.org/dtd/mybatis-3-mapper.dtd" >

<mapper namespace="$className">
    <resultMap id="$resultMap" type="$className">
        <id column="id" property="id" />
        $properties
        <result column="created_at" property="createdAt"/>
        <result column="updated_at" property="updatedAt"/>
    </resultMap>

    <sql id="table_name">
        $tableName
    </sql>

    <sql id="columns_exclude_id">
        $columns
        created_at, updated_at
    </sql>

    <sql id="columns">
        id,
        <include refid="columns_exclude_id"/>
    </sql>

    <sql id="values">
        $values
        now(),now()
    </sql>

    <sql id="criteria">
        1 = 1
        $criteria
    </sql>

    <insert id="create" parameterType="$className" keyProperty="id" useGeneratedKeys="true">
        INSERT INTO
        <include refid="table_name"/>
        (<include refid="columns_exclude_id"/>)
        VALUES (<include refid="values"/>)
    </insert>

    <update id="update" parameterType="$className">
        UPDATE
        <include refid="table_name"/>
        <set>
            $updates
            updated_at=now()
        </set>
        WHERE id=#{id}
    </update>

    <select id="findById" parameterType="long" resultMap="$resultMap">
        SELECT
        <include refid="columns"/>
        FROM
        <include refid="table_name"/>
        WHERE id = #{id} LIMIT 1
    </select>

    <select id="findByIds" parameterType="list" resultMap="$resultMap">
        SELECT
        <include refid="columns"/>
        FROM
        <include refid="table_name"/>
        WHERE id IN
        <foreach collection="list" separator="," open="(" close=")" item="id">
            #{id}
        </foreach>
    </select>

    <select id="count" parameterType="map" resultType="long">
        SELECT COUNT(1)
        FROM
        <include refid="table_name"/>
        <where>
            <include refid="criteria"/>
        </where>
    </select>

    <select id="paging" parameterType="map" resultMap="$resultMap">
        SELECT
        <include refid="columns"/>
        FROM
        <include refid="table_name"/>
        <where>
            <include refid="criteria"/>
        </where>
        ORDER BY id DESC
        <if test="limit != null">
            LIMIT
            <if test="offset != null">#{offset},</if>
            #{limit}
        </if>
    </select>

    <select id="list" parameterType="map" resultMap="$resultMap">
        SELECT
        <include refid="columns"/>
        FROM
        <include refid="table_name"/>
        <where>
            <include refid="criteria"/>
        </where>
        ORDER BY id DESC
    </select>

    <delete id="delete" parameterType="map">
        delete from <include refid="table_name"/>
        where id=#{id}
    </delete>

</mapper>
""".trim())

            sqlFields=sqlFields.trim()
            new File(className+".sql").write("""
CREATE TABLE $tableName(
    `id` BIGINT NOT NULL AUTO_INCREMENT  COMMENT '自增主键',
    $sqlFields
    `created_at`  DATETIME  NULL  COMMENT '创建时间',
    `updated_at`  DATETIME  NULL  COMMENT '修改时间',
    PRIMARY KEY (`id`)
);
""".trim())

        }catch (Exception ex){
            println(ex.getMessage())
        }
    }

    String camel2Underscore(String from){
        return CaseFormat.LOWER_CAMEL.convert(CaseFormat.LOWER_UNDERSCORE, from);
    }
}
