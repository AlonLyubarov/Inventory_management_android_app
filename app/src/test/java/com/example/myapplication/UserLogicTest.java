package com.example.myapplication;

import com.example.myapplication.model.User;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 * Unit tests for User model and role logic.
 */
public class UserLogicTest {

    @Test
    public void testUserRoleAssignment() {
        User user = new User("123", "test@test.com", "John Doe", "MANAGER", "123");
        assertEquals("MANAGER", user.getRole());
        
        user.setRole("WORKER");
        assertEquals("WORKER", user.getRole());
    }

    @Test
    public void testEmployerIdLinking() {
        String managerUid = "admin_001";
        User worker = new User("worker_001", "worker@test.com", "Bob", "WORKER", managerUid);
        
        assertEquals(managerUid, worker.getEmployerId());
        assertNotEquals(worker.getUserId(), worker.getEmployerId());
    }

    @Test
    public void testManagerSelfEmployment() {
        String uid = "manager_777";
        User manager = new User(uid, "manager@test.com", "Alon", "MANAGER", uid);
        
        assertEquals(uid, manager.getEmployerId());
        assertEquals(manager.getUserId(), manager.getEmployerId());
    }
}
