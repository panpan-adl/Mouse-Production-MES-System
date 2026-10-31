package com.mouse.mes.config;

import java.util.*;

/** Shared server-side action boundary. Role 9 is the business administrator. */
public final class RolePermissions {
    private RolePermissions() {}
    public static boolean canRead(int role, String path) {
        if (path.startsWith("/api/account/")) return role >= 1 && role <= 11;
        if (role == 9) return true;
        if (path.startsWith("/api/finance")) return Set.of(1,10).contains(role);
        if(path.startsWith("/api/roles/manage"))return role==11;
        if (path.startsWith("/api/users")) return role == 11;
        if (path.startsWith("/api/operation-logs") || path.startsWith("/api/login-logs")) return role == 11;
        return role >= 1 && role <= 11;
    }
    public static boolean canWrite(int role,String path,String method){
        if(path.startsWith("/api/account/"))return role>=1&&role<=11;
        if(role==9)return true;
        if(path.startsWith("/api/agent/"))return role>=1&&role<=11; // Each skill enforces its business roles.
        if(path.startsWith("/api/mes/"))return role>=1&&role<=11; // Each operation checks its own roles.
        if(path.startsWith("/api/ai/")||path.startsWith("/api/analytics/"))return Set.of(1,2,4,5,6,7,9,10).contains(role);
        if(path.startsWith("/api/automation"))return Set.of(1,2,4,6).contains(role);
        if(path.startsWith("/api/auth/"))return true;
        if(path.startsWith("/api/foundation/"))return true; // Fine-grained catalog checks are in its controller.
        if(path.startsWith("/api/fulfillment/"))return role==5;
        if(path.startsWith("/api/material-requests"))return (path.endsWith("/purchase-ordered")||path.endsWith("/purchase-order"))?role==6:Set.of(1,2,3,5).contains(role);
        if(path.startsWith("/api/production-tasks/")&&(path.endsWith("/claim")||path.endsWith("/start")||path.endsWith("/report")))return Set.of(1,2,3).contains(role);
        if(path.startsWith("/api/production-reports"))return Set.of(1,2,3).contains(role) && (role!=3 || method.equals("POST"));
        if(path.startsWith("/api/production-")||path.startsWith("/api/work-order")||path.startsWith("/api/boms"))return Set.of(1,2).contains(role);
        if(path.startsWith("/api/qc-")||path.startsWith("/api/quality-"))return role==4;
        if(path.startsWith("/api/equipments")||path.startsWith("/api/maintenances"))return role==8;
        if(path.startsWith("/api/purchase-order/")&&path.endsWith("/receive"))return role==5;
        if(path.startsWith("/api/purchase-")||path.startsWith("/api/suppliers"))return role==6;
        if(path.startsWith("/api/orders")||path.startsWith("/api/sales-order")||path.startsWith("/api/customer"))return role==7;
        if(path.startsWith("/api/inventory")||path.startsWith("/api/stock-")||path.startsWith("/api/warehouses")||path.startsWith("/api/locations")||path.startsWith("/api/lots")||path.startsWith("/api/material"))return role==5;
        if(path.startsWith("/api/users")||path.startsWith("/api/roles")||path.startsWith("/api/departments")||path.startsWith("/api/dicts"))return role==11;
        return false;
    }
}
