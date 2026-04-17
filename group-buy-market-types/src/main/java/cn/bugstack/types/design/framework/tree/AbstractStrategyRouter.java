package cn.bugstack.types.design.framework.tree;

public abstract class AbstractStrategyRouter <T,D,R>implements StrategyMapper<T,D,R>,StrategyHandler<T,D,R>
{
    protected StrategyHandler<T,D,R> defaultStrategyHandler=StrategyHandler.DEFAULT;

    public R router(T requestParameter, D dynamicContext) throws Exception {
        StrategyHandler<T,D,R> strategyHandler=get(requestParameter, dynamicContext);

        if(null !=strategyHandler) return strategyHandler.apply(requestParameter, dynamicContext);

        return defaultStrategyHandler.apply(requestParameter, dynamicContext);
    }
}


