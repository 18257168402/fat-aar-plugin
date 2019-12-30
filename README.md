## 说明

本插件改写自[Vigi0303/fat-aar-plugin](https://github.com/Vigi0303/fat-aar-plugin)

因为原作者很久没有更新了，他写的那个插件还是基于gralde 2.3的

而gradle3.0以后这个插件就没用了，所以我对这个库进行了更新

## 作用

当你为一个模块打包aar的时候，并不会将他的依赖库也打包进aar里面，有时候我们又需要有这样的功能，这个插件就是为此功能而生了

可以将依赖的模块，依赖的aar，依赖的jar打包进aar中



## 使用方式

```
apply plugin: 'com.zb.fat-aar'
dependencies {
    implementation fileTree(dir: 'libs', include: ['*.jar'])

    implementation 'com.android.support:appcompat-v7:28.0.0'

    def dep=project(path:":modulea");
    embed dep; //依赖模块使用embed，如果不进行打包，作用于implementation一致
    embed project(":modulejni")
    embed "com.alibaba:fastjson:1.1.60.android"//依赖的第三方aar也可以嵌入
}

apply plugin: 'maven'
uploadArchives {
    repositories.mavenDeployer {
        repository(url: 'file:../../repo')
        pom.groupId = "com.zb.flat"
        pom.artifactId = "all"
        pom.version= "1.0"

        publishFatAAR(pom)//将打包的pom传给插件
    }
}
```



### 原理

使用embed收集好各个需要嵌入的模块之后，遍历这些依赖模块的产物（产物可能是jar，可能是aar），然后合并Manifest，资源文件，R.class 等等

