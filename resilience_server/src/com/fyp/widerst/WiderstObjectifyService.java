package com.fyp.widerst;

import com.fyp.widerst.entity.DataPiece;
import com.fyp.widerst.entity.DataWhole;
import com.fyp.widerst.entity.DeviceInfo;
import com.googlecode.objectify.Objectify;
import com.googlecode.objectify.ObjectifyFactory;
import com.googlecode.objectify.ObjectifyService;

public class WiderstObjectifyService {

    static {
        factory().register(DataWhole.class);
        factory().register(DataPiece.class);
        factory().register(DeviceInfo.class);
    }
    
    public static Objectify ofy() {
        return ObjectifyService.ofy();
    }

    public static ObjectifyFactory factory() {
        return ObjectifyService.factory();
    }
    
}
