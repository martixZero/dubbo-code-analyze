/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.dubbo.config.cache;

import org.apache.dubbo.config.ApplicationConfig;
import org.apache.dubbo.config.ProtocolConfig;
import org.apache.dubbo.config.ReferenceConfig;
import org.apache.dubbo.config.RegistryConfig;
import org.apache.dubbo.config.ServiceConfig;

import junit.framework.TestCase;
import org.junit.Assert;
import org.junit.Test;

/**
 * CacheTest
 *
 *
 * dubbo缓存验证
 * 开启缓存在客户端 默认是1000次 lru算法  是指不会调用远程触发invocation的操作
 */
public class CacheTest extends TestCase {

    @Test
    public void testCache() throws Exception {
        ServiceConfig<CacheService> service = new ServiceConfig<CacheService>();
        service.setApplication(new ApplicationConfig("cache-provider"));
        service.setRegistry(new RegistryConfig("N/A"));
        service.setProtocol(new ProtocolConfig("dubbo", 29582));
        service.setInterface(CacheService.class.getName());
        service.setRef(new CacheServiceImpl());
        service.export();
        try {
            ReferenceConfig<CacheService> reference = new ReferenceConfig<CacheService>();
            reference.setApplication(new ApplicationConfig("cache-consumer"));
            reference.setInterface(CacheService.class);
            reference.setUrl("dubbo://127.0.0.1:29582?scope=remote&cache=true");
            CacheService cacheService = reference.get();
            try {
                // verify cache, same result is returned for multiple invocations (in fact, the return value increases
                // on every invocation on the server side)
                String fix = null;
                for (int i = 0; i < 3; i++) {
                    String result = cacheService.findCache("0");
                    Assert.assertTrue(fix == null || fix.equals(result));
                    fix = result;
                    Thread.sleep(100);
                }

                // default cache.size is 1000 for LRU, should have cache expired if invoke more than 1001 times
                for (int n = 0; n < 1001; n++) {
                    String pre = null;
                    for (int i = 0; i < 10; i++) {
                        String result = cacheService.findCache(String.valueOf(n));
                        Assert.assertTrue(pre == null || pre.equals(result));
                        pre = result;
                    }
                }

                // verify if the first cache item is expired in LRU cache
                String result = cacheService.findCache("0");
                Assert.assertFalse(fix == null || fix.equals(result));
            } finally {
//                销毁引用
                reference.destroy();
            }
        } finally {
//            注销服务
            service.unexport();
        }
    }

}
