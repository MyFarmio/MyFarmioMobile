package com.myfarmio.app.data.demo;
import org.junit.Test;
import java.util.*;
import static org.junit.Assert.*;
public class DemoRepositoryTest {
    @Test public void demoHasEveryVisualModuleButNoRemoteState() {
        DemoRepository repo=new DemoRepository(()->{});
        for(String key:Arrays.asList("fields","livestock","animals","finance","inventory","weather","windows","climate_alerts","profile"))assertFalse(key,repo.list(key).isEmpty());
    }
    @Test public void financeCrudIsLocalAndNotifies() {
        int[] updates={0};DemoRepository repo=new DemoRepository(()->updates[0]++);
        Map<String,Object> draft=new LinkedHashMap<>();draft.put("description","Venta");draft.put("amount",100);
        Map<String,Object> saved=repo.save("finance",draft);assertEquals(1,updates[0]);assertEquals(3,repo.list("finance").size());
        saved.put("amount",999);assertEquals(100,repo.list("finance").get(0).get("amount"));
        repo.delete("finance",saved.get("id").toString());assertEquals(2,repo.list("finance").size());assertEquals(2,updates[0]);
    }
    @Test public void herdsRemainDerivedFromAnimals() {
        DemoRepository repo=new DemoRepository(()->{});
        assertEquals(10,repo.list("livestock").get(0).get("quantity"));
        Map<String,Object> animal=repo.list("animals").get(0);
        repo.delete("animals",animal.get("id").toString());
        assertEquals(9,repo.list("livestock").get(0).get("quantity"));
    }
    @Test public void newRepositoryDoesNotInheritOldEdits() {
        DemoRepository first=new DemoRepository(()->{}),second=new DemoRepository(()->{});
        first.delete("inventory",first.list("inventory").get(0).get("id").toString());
        assertEquals(1,first.list("inventory").size());assertEquals(2,second.list("inventory").size());
    }
    @Test public void demoSupportsEmptyAndLongContentWithoutChangingKeys() {
        DemoRepository repo=new DemoRepository(()->{});
        for(Map<String,Object> row:repo.list("inventory"))repo.delete("inventory",row.get("id").toString());
        assertTrue(repo.list("inventory").isEmpty());
        Map<String,Object> draft=new LinkedHashMap<>();draft.put("name","Insumo de prueba con una descripción deliberadamente extensa para validar el salto de línea");
        repo.save("inventory",draft);assertEquals(draft.get("name"),repo.list("inventory").get(0).get("name"));
    }
}

