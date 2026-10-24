package com.mouse.mes.service;

import java.util.Set;
import java.util.function.Supplier;

/** Only source-validated fulfillment operations may create linked stock documents. */
public final class PostingScope {
    private static final ThreadLocal<Boolean> ACTIVE=ThreadLocal.withInitial(()->false);
    private PostingScope() {}
    public static <T> T execute(Supplier<T> action) {
        boolean previous=ACTIVE.get();ACTIVE.set(true);
        try{return action.get();}finally{if(previous)ACTIVE.set(true);else ACTIVE.remove();}
    }
    public static void validate(String type,Long source) {
        if(!ACTIVE.get() && (source!=null || Set.of("PURCHASE","PRODUCTION","SALES","PURCHASE_RECEIPT","PRODUCTION_RECEIPT","PRODUCTION_ISSUE","SALES_SHIPMENT","PURCHASE_RETURN","SALES_RETURN","LINE_RETURN","LINE_ISSUE").contains(type==null?"":type)))
            throw new IllegalArgumentException("关联业务单据请在生产与仓储闭环中执行，系统将校验来源、质检和重复操作");
    }
}
