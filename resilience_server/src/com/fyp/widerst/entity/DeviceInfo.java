
package com.fyp.widerst.entity;

import com.googlecode.objectify.annotation.Entity;
import com.googlecode.objectify.annotation.Id;

@Entity
public class DeviceInfo {
    @Id
    private Long mServerRegistrationId;
    private String mDeviceRegistrationID;
    
    public DeviceInfo() {
        
    }

    public String getDeviceRegistrationId() {
        return mDeviceRegistrationID;
    }

    public String getServerRegistrationId() {
        return mServerRegistrationId.toString();
    }

    public void setDeviceRegistrationId(String deviceRegistrationId) {
        mDeviceRegistrationID = deviceRegistrationId;
    }

    @Override
    public String toString() {
        return mDeviceRegistrationID;
    }
    
}
