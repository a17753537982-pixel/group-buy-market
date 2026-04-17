package cn.bugstack.types.design.framework.tree;

public abstract class AbstractMultiThreadStateRouter<T,D,R> extends AbstractStrategyRouter<T,D,R>{
    @Override
    public R apply(T requestParameter, D dynamicContext) throws Exception {
        multiThread( requestParameter,  dynamicContext);
        return doApply(requestParameter,  dynamicContext);
    }

    /**
     * 异步数据获取
     * @param requestParameter
     * @param dynamicContext
     */
    protected abstract void multiThread(T requestParameter, D dynamicContext) ;

    /**
     * 业务逻辑受理
     * @param requestParameter
     * @param dynamicContext
     * @return
     * @throws Exception
     */
    public abstract R doApply(T requestParameter, D dynamicContext) throws Exception;
}
