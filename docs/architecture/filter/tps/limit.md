

dubbo filter限流策略：

参数：次数  最近一次计算开始时间点  时间间隔    


/**
     * 是否允许请求
     * @return
     */
    public boolean isAllowable() {
        // 1.判断是否过了指定的时间间隔 过了则进行时间点重置 及次数重置
        long now = System.currentTimeMillis();
        if (now > lastResetTime + interval) {
            token.set(rate);
            lastResetTime = now;
        }
        // 获取剩余次数
        int value = token.get();
        boolean flag = false;
        // 轮询重试  CAS方式 如果允许则会进行计数减一 防止多线程进行并发修改
        while (value > 0 && !flag) {
            // CAS失败则为false 获取value进行不断重试
            flag = token.compareAndSet(value, value - 1);
            value = token.get();
        }
        return flag;
    }
dubbo对于服务调用的限流过程：

在filter的invoke方法中调用tps限制校验：如果超过限制则抛出超过最大限制异常
    if (!tpsLimiter.isAllowable(invoker.getUrl(), invocation)) {
                throw new RpcException(
                        "Failed to invoke service " +
                                invoker.getInterface().getName() +
                                "." +
                                invocation.getMethodName() +
                                " because exceed max service tps.");
        

public class DefaultTPSLimiter implements TPSLimiter {
    private final ConcurrentMap<String, StatItem> stats
            = new ConcurrentHashMap<String, StatItem>();
    @Override
    public boolean isAllowable(URL url, Invocation invocation) {
        // 获取配置的tps限制阈值
        int rate = url.getParameter(Constants.TPS_LIMIT_RATE_KEY, -1);
        // 获取默认时间间隔
        long interval = url.getParameter(Constants.TPS_LIMIT_INTERVAL_KEY,
                Constants.DEFAULT_TPS_LIMIT_INTERVAL);
                // 服务名称作为key
        String serviceKey = url.getServiceKey();
        // 表示设置了限流阈值
        if (rate > 0) {
            StatItem statItem = stats.get(serviceKey);
            if (statItem == null) {
                stats.putIfAbsent(serviceKey,
                        new StatItem(serviceKey, rate, interval));
                statItem = stats.get(serviceKey);
            }
            return statItem.isAllowable();
        } else {
            // 期间发生变更 不使用了阈值，则移除掉
            StatItem statItem = stats.get(serviceKey);
            if (statItem != null) {
                stats.remove(serviceKey);
            }
        }
        return true;
    }

}




