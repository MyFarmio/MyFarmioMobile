package com.myfarmio.app.ui.common;

import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;
import com.myfarmio.app.auth.SessionManager;
import com.myfarmio.app.data.demo.DemoRepository;

/** Activity-scoped frontend workspace; no changes to authentication or permissions. */
public class WorkspaceViewModel extends ViewModel {
    public final MutableLiveData<Integer> revision = new MutableLiveData<>(0);
    private String user;
    private boolean demo;
    private boolean demoAccount;
    private DemoRepository repository;
    public void prepare(SessionManager session) {
        String next = session.getUserId();
        if (repository != null && java.util.Objects.equals(user,next)) return;
        user = next;
        demoAccount = "demo-user-id".equals(next);
        demo = demoAccount;
        repository = new DemoRepository(this::notifyChanged);
        notifyChanged();
    }
    public boolean isDemo() { return demo; }
    public boolean isDemoAccount() { return demoAccount; }
    public DemoRepository records() { return repository; }
    public void setDemo(boolean enabled) {
        demo = demoAccount || enabled;
        notifyChanged();
    }
    public void clear() { repository = null; user = null; demo = false; demoAccount = false; }
    private void notifyChanged() { revision.setValue(revision.getValue() == null ? 1 : revision.getValue() + 1); }
}
