package org.tiqr.authenticator;

import java.util.Arrays;
import java.util.List;

import dagger.ObjectGraph;

/**
 * Tiqr Application base.
 */
public class Application extends android.app.Application {
    private ObjectGraph _objectGraph;

    @Override
    public void onCreate() {
        super.onCreate();
        _objectGraph = ObjectGraph.create(_getModules().toArray());
        _objectGraph.inject(this);
    }

    private List<Object> _getModules() {
        return Arrays.<Object>asList(new ApplicationModule(this));
    }

    public <T> T inject(T instance) {
        return _objectGraph.inject(instance);
    }

    public ObjectGraph createScopedGraph(Object... modules) {
        return _objectGraph.plus(modules);
    }
}
