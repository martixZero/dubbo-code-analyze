

对外配置接口，以 ServiceConfig, ReferenceConfig 为中心，可以直接初始化配置类，也可以通过 spring 解析配置生成配置类



proxy 服务代理层：服务接口透明代理，生成服务的客户端 Stub 和服务器端 Skeleton, 以 ServiceProxy 为中心，扩展接口为 ProxyFactory
registry 注册中心层：封装服务地址的注册与发现，以服务 URL 为中心，扩展接口为 RegistryFactory, Registry, RegistryService
cluster 路由层：封装多个提供者的路由及负载均衡，并桥接注册中心，以 Invoker 为中心，扩展接口为 Cluster, Directory, Router, LoadBalance
monitor 监控层：RPC 调用次数和调用时间监控，以 Statistics 为中心，扩展接口为 MonitorFactory, Monitor, MonitorService
protocol 远程调用层：封装 RPC 调用，以 Invocation, Result 为中心，扩展接口为 Protocol, Invoker, Exporter
exchange 信息交换层：封装请求响应模式，同步转异步，以 Request, Response 为中心，扩展接口为 Exchanger, ExchangeChannel, ExchangeClient, ExchangeServer
transport 网络传输层：抽象 mina 和 netty 为统一接口，以 Message 为中心，扩展接口为 Channel, Transporter, Client, Server, Codec
serialize 数据序列化层：可复用的一些工具，扩展接口为 Serialization, ObjectInput, ObjectOutput, ThreadPool


关系说明:
在 RPC 中，Protocol 是核心层，也就是只要有 Protocol + Invoker + Exporter 就可以完成非透明的 RPC 调用，然后在 Invoker 的主过程上 Filter 拦截点。
图中的 Consumer 和 Provider 是抽象概念，只是想让看图者更直观的了解哪些类分属于客户端与服务器端，不用 Client 和 Server 的原因是 Dubbo 
在很多场景下都使用 Provider, Consumer, Registry, Monitor 划分逻辑拓普节点，保持统一概念。
而 Cluster 是外围概念，所以 Cluster 的目的是将多个 Invoker 伪装成一个 Invoker，这样其它人只要关注 Protocol 层 Invoker 即可，
加上 Cluster 或者去掉 Cluster 对其它层都不会造成影响，因为只有一个提供者时，是不需要 Cluster 的。
Proxy 层封装了所有接口的透明化代理，而在其它层都以 Invoker 为中心，只有到了暴露给用户使用时，才用 Proxy 将 Invoker 转成接口，
或将接口实现转成 Invoker，也就是去掉 Proxy 层 RPC 是可以 Run 的，只是不那么透明，不那么看起来像调本地服务一样调远程服务。
而 Remoting 实现是 Dubbo 协议的实现，如果你选择 RMI 协议，整个 Remoting 都不会用上，Remoting 内部再划为 Transport 传输层和 Exchange 
信息交换层，Transport 层只负责单向消息传输，是对 Mina, Netty, Grizzly 的抽象，它也可以扩展 UDP 传输，而 Exchange 层是在传输层之上封装了 
Request-Response 语义。
Registry 和 Monitor 实际上不算一层，而是一个独立的节点，只是为了全局概览，用层的方式画在一起

dubbo-common 公共逻辑模块：包括 Util 类和通用模型。
dubbo-remoting 远程通讯模块：相当于 Dubbo 协议的实现，如果 RPC 用 RMI协议则不需要使用此包。
dubbo-rpc 远程调用模块：抽象各种协议，以及动态代理，只包含一对一的调用，不关心集群的管理。
dubbo-cluster 集群模块：将多个服务提供方伪装为一个提供方，包括：负载均衡, 容错，路由等，集群的地址列表可以是静态配置的，也可以是由注册中心下发。
dubbo-registry 注册中心模块：基于注册中心下发地址的集群方式，以及对各种注册中心的抽象。
dubbo-monitor 监控模块：统计服务调用次数，调用时间的，调用链跟踪的服务。
dubbo-config 配置模块：是 Dubbo 对外的 API，用户通过 Config 使用D ubbo，隐藏 Dubbo 所有细节。
dubbo-container 容器模块：是一个 Standlone 的容器，以简单的 Main 加载 Spring 启动，因为服务通常不需要 Tomcat/JBoss 等 Web 容器的
特性，没必要用 Web 容器去加载服务。

体上按照分层结构进行分包，与分层的不同点在于：

container 为服务容器，用于部署运行服务，没有在层中画出。
protocol 层和 proxy 层都放在 rpc 模块中，这两层是 rpc 的核心，在不需要集群也就是只有一个提供者时，可以只使用这两层完成 rpc 调用。
transport 层和 exchange 层都放在 remoting 模块中，为 rpc 调用的通讯基础。
serialize 层放在 common 模块中，以便更大程度复用。


http://dubbo.apache.org/docs/zh-cn/dev/sources/images/dubbo-relation.jpg

图例说明：

图中小方块 Protocol, Cluster, Proxy, Service, Container, Registry, Monitor 代表层或模块，蓝色的表示与业务有交互，绿色的表示只对 Dubbo 内部交互。
图中背景方块 Consumer, Provider, Registry, Monitor 代表部署逻辑拓扑节点。
图中蓝色虚线为初始化时调用，红色虚线为运行时异步调用，红色实线为运行时同步调用。
图中只包含 RPC 的层，不包含 Remoting 的层，Remoting 整体都隐含在 Protocol 中。



领域模型

        在 Dubbo 的核心领域模型中：
        
        Protocol 是服务域，它是 Invoker 暴露和引用的主功能入口，它负责 Invoker 的生命周期管理。
        Invoker 是实体域，它是 Dubbo 的核心模型，其它模型都向它靠扰，或转换成它，它代表一个可执行体，可向它发起 invoke 调用，它有可能是一个本地的实现，也可能是一个远程的实现，也可能一个集群实现。
        Invocation 是会话域，它持有调用过程中的变量，比如方法名，参数等。





扩展点加载
    扩展点配置
    1.来源：
    Dubbo 的扩展点加载从 JDK 标准的 SPI (Service Provider Interface) 扩展点发现机制加强而来。
    Dubbo 改进了 JDK 标准的 SPI 的以下问题：
        JDK 标准的 SPI 会一次性实例化扩展点所有实现，如果有扩展实现初始化很耗时，但如果没用上也加载，会很浪费资源。
        如果扩展点加载失败，连扩展点的名称都拿不到了。比如：JDK 标准的 ScriptEngine，通过 getName() 获取脚本类型的名称，但如果 RubyScriptEngine 因为所依赖的 jruby.jar 不存在，导致 RubyScriptEngine 类加载失败，这个失败原因被吃掉了，和 ruby 对应不起来，当用户执行 ruby 脚本时，会报不支持 ruby，而不是真正失败的原因。
        增加了对扩展点 IoC 和 AOP 的支持，一个扩展点可以直接 setter 注入其它扩展点。
    2.约定：
        在扩展类的 jar 包内 [1]，放置扩展点配置文件 META-INF/dubbo/接口全限定名，内容为：配置名=扩展实现类全限定名，多个实现类用换行符分隔。
        以扩展 Dubbo 的协议为例，在协议的实现 jar 包内放置文本文件：META-INF/dubbo/com.alibaba.dubbo.rpc.Protocol，内容为：
        xxx=com.alibaba.xxx.XxxProtocol
        package com.alibaba.xxx;  
        import com.alibaba.dubbo.rpc.Protocol;
        public class XxxProtocol implemenets Protocol { 
            // ...
        }
    配置模块中的配置
        Dubbo 配置模块中，扩展点均有对应配置属性或标签，通过配置指定使用哪个扩展实现。比如：
        <dubbo:protocol name="xxx" />
扩展点特性
    扩展点自动包装
    自动包装扩展点的 Wrapper 类。ExtensionLoader 在加载扩展点时，如果加载到的扩展点有拷贝构造函数，则判定为扩展点 Wrapper 类
    Wrapper类内容：
    package com.alibaba.xx
    import com.alibaba.dubbo.rpc.Protocol;
    public class XxxProtocolWrapper implemenets Protocol {
        Protocol impl;
        public XxxProtocol(Protocol protocol) { impl = protocol; }
        // 接口方法做一个操作后，再调用extension的方法
        public void refer() {
            //... 一些操作
            impl.refer();
            // ... 一些操作
        }
        // ...
    }
    Wrapper 类同样实现了扩展点接口，但是 Wrapper 不是扩展点的真正实现。它的用途主要是用于从 ExtensionLoader 返回扩展点时，包装在真正
    的扩展点实现外。即从 ExtensionLoader 中返回的实际上是 Wrapper 类的实例，Wrapper 持有了实际的扩展点实现类。
    扩展点的 Wrapper 类可以有多个，也可以根据需要新增。
    通过 Wrapper 类可以把所有扩展点公共逻辑移至 Wrapper 中。新加的 Wrapper 在所有的扩展点上添加了逻辑，有些类似 AOP
    ，即 Wrapper 代理了扩展点。
    扩展点自动装配
加载扩展点时，自动注入依赖的扩展点。加载扩展点时，扩展点实现类的成员如果为其它扩展点类型，ExtensionLoader 在会自动注入依赖的扩展点。
ExtensionLoader 通过扫描扩展点实现类的所有 setter 方法来判定其成员。即 ExtensionLoader 会执行扩展点的拼装操作。
示例：有两个为扩展点 CarMaker（造车者）、WheelMaker (造轮者)
public interface CarMaker {
    Car makeCar();
}
public interface WheelMaker {
    Wheel makeWheel();
}

CarMaker 的一个实现类：
public class RaceCarMaker implemenets CarMaker {
    WheelMaker wheelMaker;
 
    public setWheelMaker(WheelMaker wheelMaker) {
        this.wheelMaker = wheelMaker;
    }
 
    public Car makeCar() {
        // ...
        Wheel wheel = wheelMaker.makeWheel();
        // ...
        return new RaceCar(wheel, ...);
    }
}
ExtensionLoader 加载 CarMaker 的扩展点实现 RaceCar 时，setWheelMaker 方法的 WheelMaker 也是扩展点则会注入 WheelMaker 的实现。
这里带来另一个问题，ExtensionLoader 要注入依赖扩展点时，如何决定要注入依赖扩展点的哪个实现。在这个示例中，即是在多个WheelMaker 
的实现中要注入哪个。
这个问题在下面一点 扩展点自适应 中说明。
扩展点自适应
    ExtensionLoader 注入的依赖扩展点是一个 Adaptive 实例，直到扩展点方法执行时才决定调用是一个扩展点实现。
    Dubbo 使用 URL 对象（包含了Key-Value）传递配置信息。
    扩展点方法调用会有URL参数（或是参数有URL成员）
    这样依赖的扩展点也可以从URL拿到配置信息，所有的扩展点自己定好配置的Key后，配置信息从URL上从最外层传入。URL在配置传递上即是一条总线。
    示例：有两个为扩展点 CarMaker、WheelMaker
    public interface CarMaker {
        Car makeCar(URL url);
    }
    public interface WheelMaker {
        Wheel makeWheel(URL url);
    }
    CarMaker 的一个实现类：
    public class RaceCarMaker implemenets CarMaker {
        WheelMaker wheelMaker;
        public setWheelMaker(WheelMaker wheelMaker) {
            this.wheelMaker = wheelMaker;
        }
        public Car makeCar(URL url) {
            // ...
            Wheel wheel = wheelMaker.makeWheel(url);
            // ...
            return new RaceCar(wheel, ...);
        }
    }
    当上面执行 Wheel wheel = wheelMaker.makeWheel(url);
    时，注入的 Adaptive 实例可以提取约定 Key 来决定使用哪个 WheelMaker 实现来调用对应实现的真正的 makeWheel 方法。如提取 wheel.type
    , key 即 url.get("wheel.type") 来决定 WheelMake 实现。Adaptive 实例的逻辑是固定，指定提取的 URL 的 Key，即可以代理真正的实现
    类上，可以动态生成。
 在 Dubbo 的 ExtensionLoader 的扩展点类对应的 Adaptive 实现是在加载扩展点里动态生成。指定提取的 URL 的 Key 通过 @Adaptive 
 注解在接口方法上提供。
    下面是 Dubbo 的 Transporter 扩展点的代码：
    public interface Transporter {
        @Adaptive({"server", "transport"})
        Server bind(URL url, ChannelHandler handler) throws RemotingException;
        @Adaptive({"client", "transport"})
        Client connect(URL url, ChannelHandler handler) throws RemotingException;
    }
    对于 bind() 方法，Adaptive 实现先查找 server key，如果该 Key 没有值则找 transport key 值，来决定代理到哪个实际扩展点。
扩展点自动激活
    对于集合类扩展点，比如：Filter, InvokerListener, ExportListener, TelnetHandler, StatusChecker 等，可以同时加载多个实现，
    此时，可以用自动激活来简化配置，如：
    import com.alibaba.dubbo.common.extension.Activate;
    import com.alibaba.dubbo.rpc.Filter;
    @Activate // 无条件自动激活
    public class XxxFilter implements Filter {
        // ...
    }
    import com.alibaba.dubbo.common.extension.Activate;
    import com.alibaba.dubbo.rpc.Filter; 
    @Activate("xxx") // 当配置了xxx参数，并且参数为有效值时激活，比如配了cache="lru"，自动激活CacheFilter。
    public class XxxFilter implements Filter {
        // ...
    }
    import com.alibaba.dubbo.common.extension.Activate;
    import com.alibaba.dubbo.rpc.Filter;
    @Activate(group = "provider", value = "xxx") // 只对提供方激活，group可选"provider"或"consumer"
    public class XxxFilter implements Filter {
        // ...
    }
    注意：这里的配置文件是放在你自己的 jar 包内，不是 dubbo 本身的 jar 包内，Dubbo 会全 ClassPath 扫描所有 jar 包内同名的这个文件，然后进行合并 ↩︎
    注意：扩展点使用单一实例加载（请确保扩展实现的线程安全性），缓存在 ExtensionLoader 中 
    
实现细节
    初始化过程细节
        解析服务
        基于 dubbo.jar 内的 META-INF/spring.handlers 配置，Spring 在遇到 dubbo 名称空间时，会回调 DubboNamespaceHandler。
        所有 dubbo 的标签，都统一用 DubboBeanDefinitionParser 进行解析，基于一对一属性映射，将 XML 标签解析为 Bean 对象。
        在 ServiceConfig.export() 或 ReferenceConfig.get() 初始化时，将 Bean 对象转换 URL 格式，所有 Bean 属性转成 URL 的参数。
        然后将 URL 传给 协议扩展点，基于扩展点的 扩展点自适应机制，根据 URL 的协议头，进行不同协议的服务暴露或引用。
        DubboNamespaceHandler：继承了spring的NamespaceHandlerSupport 重写了init初始化方法，向容器里进行注册不同的Bean解析器
        registerBeanDefinitionParser("application", new DubboBeanDefinitionParser(ApplicationConfig.class, true));
        暴露服务
        1. 只暴露服务端口：
        在没有注册中心，直接暴露提供者的情况下 [1]，ServiceConfig 解析出的 URL 的格式为： dubbo://service-host/com.foo.FooService?version=1.0.0。
        基于扩展点自适应机制，通过 URL 的 dubbo:// 协议头识别，直接调用 DubboProtocol的 export() 方法，打开服务端口。
        2. 向注册中心暴露服务：
        在有注册中心，需要注册提供者地址的情况下 [2]，ServiceConfig 解析出的 URL 的格式为: registry://registry-host/com.alibaba.dubbo.registry.RegistryService?export=URL.encode("dubbo://service-host/com.foo.FooService?version=1.0.0")，
        基于扩展点自适应机制，通过 URL 的 registry:// 协议头识别，就会调用 RegistryProtocol 的 export() 方法，将 export 参数中的提供者 URL，先注册到注册中心。
        再重新传给 Protocol 扩展点进行暴露： dubbo://service-host/com.foo.FooService?version=1.0.0，然后基于扩展点自适应机制，通过提供者 URL 的 dubbo:// 协议头识别，就会调用 DubboProtocol 的 export() 方法，打开服务端口。
引用服务
1. 直连引用服务：
在没有注册中心，直连提供者的情况下 [3]，ReferenceConfig 解析出的 URL 的格式为：dubbo://service-host/com.foo.FooService?version=1.0.0。

基于扩展点自适应机制，通过 URL 的 dubbo:// 协议头识别，直接调用 DubboProtocol 的 refer() 方法，返回提供者引用。

2. 从注册中心发现引用服务：
在有注册中心，通过注册中心发现提供者地址的情况下 [4]，ReferenceConfig 解析出的 URL 的格式为： registry://registry-host/com.alibaba.dubbo.registry.RegistryService?refer=URL.encode("consumer://consumer-host/com.foo.FooService?version=1.0.0")。

基于扩展点自适应机制，通过 URL 的 registry:// 协议头识别，就会调用 RegistryProtocol 的 refer() 方法，基于 refer 参数中的条件，查询提供者 URL，如： dubbo://service-host/com.foo.FooService?version=1.0.0。

基于扩展点自适应机制，通过提供者 URL 的 dubbo:// 协议头识别，就会调用 DubboProtocol 的 refer() 方法，得到提供者引用。

然后 RegistryProtocol 将多个提供者引用，通过 Cluster 扩展点，伪装成单个提供者引用返回。

拦截服务
基于扩展点自适应机制，所有的 Protocol 扩展点都会自动套上 Wrapper 类。

基于 ProtocolFilterWrapper 类，将所有 Filter 组装成链，在链的最后一节调用真实的引用。

基于 ProtocolListenerWrapper 类，将所有 InvokerListener 和 ExporterListener 组装集合，在暴露和引用前后，进行回调。

包括监控在内，所有附加功能，全部通过 Filter 拦截实现。
服务提供者暴露一个服务的详细过程


上图是服务提供者暴露服务的主过程：

首先 ServiceConfig 类拿到对外提供服务的实际类 ref(如：HelloWorldImpl),然后通过 ProxyFactory 类的 getInvoker 方法使用 ref 生成一
个 AbstractProxyInvoker 实例，到这一步就完成具体服务到 Invoker 的转化。接下来就是 Invoker 转换到 Exporter 的过程。

Dubbo 处理服务暴露的关键就在 Invoker 转换到 Exporter 的过程，上图中的红色部分。下面我们以 Dubbo 和 RMI 这两种典型协议的实现来进行说明：


Dubbo 的实现
Dubbo 协议的 Invoker 转为 Exporter 发生在 DubboProtocol 类的 export 方法，它主要是打开 socket 侦听服务，并接收客户端发来的各种请求，通讯细节由 Dubbo 自己实现。

RMI 的实现
RMI 协议的 Invoker 转为 Exporter 发生在 RmiProtocol类的 export 方法，它通过 Spring 或 Dubbo 或 JDK 来实现 RMI 服务，通讯细节这一块由 JDK 底层来实现，这就省了不少工作量。

        上图是服务消费的主过程：
        
        首先 ReferenceConfig 类的 init 方法调用 Protocol 的 refer 方法生成 Invoker 实例(如上图中的红色部分)，这是服务消费的关键。接下来把 Invoker 转换为客户端需要的接口(如：HelloWorld)。
        
        关于每种协议如 RMI/Dubbo/Web service 等它们在调用 refer 方法生成 Invoker 实例的细节和上一章节所描述的类似。
满眼都是 Invoker
由于 Invoker 是 Dubbo 领域模型中非常重要的一个概念，很多设计思路都是向它靠拢。这就使得 Invoker 渗透在整个实现代码里，对于刚开始接触 Dubbo 的人，确实容易给搞混了。 下面我们用一个精简的图来说明最重要的两种 Invoker：服务提供 Invoker 和服务消费 Invoker：

