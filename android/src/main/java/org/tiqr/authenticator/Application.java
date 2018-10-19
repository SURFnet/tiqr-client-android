package org.tiqr.authenticator;

import android.text.TextUtils;

import com.google.firebase.iid.FirebaseInstanceId;

import org.tiqr.service.notification.NotificationService;

import java.util.Arrays;
import java.util.List;

import javax.inject.Inject;

import dagger.ObjectGraph;

/**
 * Tiqr Application base.
 */
public class Application extends android.app.Application {
    private ObjectGraph _objectGraph;

    @Inject NotificationService _notificationService;

    @Override
    public void onCreate() {
        super.onCreate();
        _objectGraph = ObjectGraph.create(_getModules().toArray());
        _objectGraph.inject(this);

        String token = FirebaseInstanceId.getInstance().getToken();
        if (token != null && !TextUtils.isEmpty(token)) {
            _notificationService.sendRequestWithDeviceToken(token);
        }
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
