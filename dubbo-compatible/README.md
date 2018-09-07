### dubbo-compatible

Hi, all

From 2.7.x, `Dubbo` has renamed package to `org.apache.dubbo`, so `dubbo-compatible` module is provided.

For compatibility with older versions, we provider the following most popular APIs(classes/interfaces):

* org.apache.dubbo.rpc.Filter / Invocation / Invoker / Result / RpcContext / RpcException
* org.apache.dubbo.config.annotation.Reference / Service
* org.apache.dubbo.config.spring.context.annotation.EnableDubbo
* org.apache.dubbo.common.Constants / URL
* org.apache.dubbo.common.extension.ExtensionFactory
* org.apache.dubbo.common.serialize.Serialization / ObjectInput / ObjectOutput
* org.apache.dubbo.cache.CacheFactory / Cache
* org.apache.dubbo.rpc.service.EchoService / GenericService

The above APIs work fine with some unit tests in the test root. 

Except these APIs, others provided in `dubbo-compatible` are just bridge APIs without any unit tests, they may work with wrong. If you have any demand for them, you could: 

* Implement your own extensions with new APIs. (RECOMMENDED) 
* Follow `org.apache.dubbo.rpc.Filter` to implement bridge APIs, and then contribute to community. 
* Open issue on github.

By the way, We will remove this module some day, so it's recommended that implementing your extensions with new APIs at the right time. 

Now we need your help: Any other popular APIs are missing?

For compatible module, any suggestions are welcome. Thanks.