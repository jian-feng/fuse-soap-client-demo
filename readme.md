# Fuseサンプル - WebサービスのClient実装


<!-- @import "[TOC]" {cmd="toc" depthFrom=2 depthTo=6 orderedList=false} -->

<!-- code_chunk_output -->

- [ソフトウェアバージョン](#ソフトウェアバージョン)
- [本サンプルについて](#本サンプルについて)
- [処理概要](#処理概要)
  - [1. cxfEndpoint: orderService](#1-cxfendpoint-orderservice)
  - [2. Bean Processor: MyUtil.prepareSoapRequest()](#2-bean-processor-myutilpreparesoaprequest)
  - [3. Camel Route: simple-route](#3-camel-route-simple-route)
- [本サンプルの実行方法](#本サンプルの実行方法)
  - [1. ローカルPCで実行する場合](#1-ローカルpcで実行する場合)
- [ゼロから作成の手順](#ゼロから作成の手順)

<!-- /code_chunk_output -->

## ソフトウェアバージョン

- Fuse 7.x (ローカル、またはOpenshift上)
- Spring-boot 2.1
- Java 1.8

## 本サンプルについて

本サンプルでは、[camel-cxfコンポーネント][1]を使って、SOAP Webサービス(Client)の実装方法を示します。SOAP Serverの実装は、[fuse-soap-server-demo][3]を参照してください。

[3]: https://github.com/jian-feng/fuse-soap-server-demo

Webサービスの開発手法は、Coding-FirstとWSDL-Fisrtの２種類がありますが、
今回は、素早くWebサービスのクライアントを実装ため、Coding-Firstの手法を採用しました。

また、[camel-cxf][1]のオプションが非常に多いですが、今回は初心者向けのため、最低限の設定のみとなっています。他の詳細なオプションは[リンク先][1]の製品ドキュメントをご参照ください。

[1]: https://access.redhat.com/documentation/en-us/red_hat_fuse/7.5/html/apache_camel_component_reference/cxf-component

## 処理概要

### 1. cxfEndpoint: orderService

cxfEndpoint Beanを使って、orderService(Webサービスのインタフェース)をWebサービスに公開します。
```xml
<cxf:cxfEndpoint id="orderService"
  address="http://localhost:9090/order/"
  serviceClass="com.sample.OrderService"/>
```

上記の`com.sample.OrderService`では、Javaインタフェースを使ったWebサービスの定義となっています。
```java
@WebService
public interface OrderService {
  String order(
    @WebParam(name = "product") String product,
    @WebParam(name = "amount") int amount
    );
}
```

### 2. Bean Processor: MyUtil.prepareSoapRequest()

上記のWebサービスの`order` Operationを呼び出しの前に、パラメータとオペレーション名を事前に用意する必要があります。

今回はBean Processorで実装しました。
File: `src/main/java/com/sample/MyUtil.java`
```java
@Component("myUtil")
public class MyUtil {

    public void prepareSoapRequest(Exchange exchange) {
        // prepare parameters for SOAP Request
        List<Object> params = Arrays.asList("test", 10);
        exchange.getIn().setBody(params);

        // set operation of SOAP Request
        exchange.getIn().setHeader(CxfConstants.OPERATION_NAME, "order");
    }
}
```

後ほどのCamel Route内で以下のようにBeanを呼び出します。
```xml
<bean ref="myUtil" method="prepareSoapRequest"/>
```

複雑なパラメータのセットの仕方はCamel-cxfの[テストコード][2]を参考にしてください。

[2]: https://github.com/apache/camel/blob/master/components/camel-cxf/src/test/java/org/apache/camel/component/cxf/CxfProducerOperationTest.java

### 3. Camel Route: simple-route

このCamel Routeは、2秒間隔でcxfEndpointに対してSOAPリクエストを送信します。

```xml
<route id="simple-route">
  <from id="route-timer" uri="timer:foo?period=2000" />
  <bean ref="myUtil" method="prepareSoapRequest"/>
  <to uri="cxf:bean:orderService" />
  <log message=">>> Response is ${body}" />
</route>
```

## 本サンプルの実行方法

### 1. ローカルPCで実行する場合

前提条件:
- Maven 3.x がローカルPCでインストール済みであること
- Maven Remote Repositoryがアクセスできること
- [fuse-soap-server-demo][3]が起動されていること

実行手順:
1. コマンドプロンプトにて実行します。  
   `mvn clean spring-boot:run`

2. コマンドプロンプトで2秒間隔にWebサービスのレスポンスを確認してください。  
   ```
   INFO  simple-route - >>> Response is 453
   ```


## ゼロから作成の手順

以下は本サンプルをゼロから作成する場合の手順を説明します。

※ 本手順の実施には、インターネット接続が必要。

1. Maven archetypeより、Fuseアプリの雛形を生成します。  
    ```sh
    # Config archetype to use
    archetypeVersion=2.2.0.fuse-sb2-750011-redhat-00006
    archetypeCatalog=https://maven.repository.redhat.com/ga/io/fabric8/archetypes/archetypes-catalog/${archetypeVersion}/archetypes-catalog-${archetypeVersion}-archetype-catalog.xml

    # Config Fuse project name to generate
    MY_PROJECT=fuse-soap-client-demo

    # Generate from archetype, which is SpringBoot2 based Camel Application using XML DSL
    mvn org.apache.maven.plugins:maven-archetype-plugin:2.4:generate -B \
      -DarchetypeCatalog=${archetypeCatalog} \
      -DarchetypeGroupId=org.jboss.fuse.fis.archetypes \
      -DarchetypeVersion=${archetypeVersion} \
      -DarchetypeArtifactId=spring-boot-camel-xml-archetype \
      -DgroupId=com.sample \
      -DartifactId=${MY_PROJECT}  \
      -DoutputDirectory=${MY_PROJECT}
    ```

2.  依存ライブラリcamel-cxfとcxf-runtime-transportを追加  
    File: `pom.xml`
    ```xml
    <!-- Camel with CXF on Spring-boot -->
    <dependency>
        <groupId>org.apache.camel</groupId>
        <artifactId>camel-cxf-starter</artifactId>
    </dependency>
    <dependency>
        <groupId>org.apache.cxf</groupId>
        <artifactId>cxf-rt-transports-http-undertow</artifactId>
    </dependency>
    ```

3. WebServiceインターフェイスを作成  
    File: `src/main/java/com/sample/OrderService.java`
    ```java
    @WebService
    public interface OrderService {

      String order(
        @WebParam(name = "product") String product,
        @WebParam(name = "amount") int amount
        );
    }
    ```
    WebServiceインターフェイスの細かいカスタマイズ方法は、以下を参照してください。
    - http://terasolunaorg.github.io/guideline/5.5.1.RELEASE/ja/ArchitectureInDetail/WebServiceDetail/SOAP.html#web  
      ==> WebServiceインターフェイスの作成

    - https://cwiki.apache.org/confluence/pages/viewpage.action?pageId=83179  
      ==> サービスの実装

4. Bean Processorを作成  
     File: `src/main/java/com/sample/MyUtil.java`
     ```java
     package com.sample;

    import java.util.Arrays;
    import java.util.List;

    import org.apache.camel.Exchange;
    import org.apache.camel.component.cxf.common.message.CxfConstants;
    import org.springframework.stereotype.Component;

    @Component("myUtil")
    public class MyUtil {

        /**
        * Bean Processor to prepare SOAP Request
        * @param exchange
        */
        public void prepareSoapRequest(Exchange exchange) {
            // prepare parameters for SOAP Request
            List<Object> params = Arrays.asList("test", 10);
            exchange.getIn().setBody(params);

            // set operation of SOAP Request
            exchange.getIn().setHeader(CxfConstants.OPERATION_NAME, "order");
        }

    }
    ```

5. camel-contextのXMLネームスペースを修正  
    `xmlns:cxf`と`xsi:schemaLocation`を以下のように追加します。  
    File: `src/main/resources/spring/camel-context.xml`  
    ```xml
    <beans xmlns="http://www.springframework.org/schema/beans"
           xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
           xmlns:cxf="http://camel.apache.org/schema/cxf"
           xsi:schemaLocation="
           http://www.springframework.org/schema/beans http://www.springframework.org/schema/beans/spring-beans.xsd
           http://camel.apache.org/schema/spring http://camel.apache.org/schema/spring/camel-spring.xsd
           http://camel.apache.org/schema/cxf http://camel.apache.org/schema/cxf/camel-cxf.xsd">
    ```

6. camel-contextへcxfエンドポイントのbean定義を追加  
    File: `src/main/resources/spring/camel-context.xml`  
    ```xml
    <cxf:cxfEndpoint id="orderService"
      address="http://localhost:9090/order/"
      serviceClass="com.sample.OrderService"/>
    ```

7. camel-context内のCamel Routeをすべて削除して、以下のRouteを追加  
    File: `src/main/resources/spring/camel-context.xml`  
    ```xml
    <route id="simple-route">
      <from id="route-timer" uri="timer:foo?period=2000" />
      <bean ref="myUtil" method="prepareSoapRequest"/>
      <to uri="cxf:bean:orderService" />
      <log message=">>> Response is ${body}" />
    </route>
    ```
