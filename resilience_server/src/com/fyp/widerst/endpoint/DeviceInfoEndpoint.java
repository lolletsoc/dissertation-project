
package com.fyp.widerst.endpoint;

import static com.fyp.widerst.WiderstObjectifyService.ofy;

import javax.inject.Named;

import com.fyp.widerst.entity.DeviceInfo;
import com.fyp.widerst.util.DbHelper;
import com.google.api.server.spi.config.Api;
import com.google.api.server.spi.config.ApiMethod;
import com.google.api.server.spi.config.ApiMethod.HttpMethod;
import com.googlecode.objectify.Work;

@Api(name = "register", description = "Endpoint for clients to register for a unique ID abstracted from their GCM ID")
public class DeviceInfoEndpoint {

    /**
     * This inserts the entity into App Engine datastore. It uses HTTP POST
     * method.
     * 
     * @param deviceinfo the entity to be inserted.
     * @return The inserted entity.
     */
    @ApiMethod(name = "devices.insert", httpMethod = HttpMethod.POST, path = "devices")
    public DeviceInfo insertDeviceInfo(DeviceInfo deviceInfo) {
        
        ofy().save().entity(deviceInfo).now();
        return deviceInfo;
    }

    /**
     * This method is used for updating a entity. It uses HTTP PUT method.
     * 
     * @param deviceinfo the entity to be updated.
     * @return The updated entity.
     */
    @ApiMethod(name = "devices.update", httpMethod = HttpMethod.PUT, path = "devices")
    public DeviceInfo updateDeviceInfo(DeviceInfo deviceInfo) {
        
        final DeviceInfo queryDevice = DbHelper.findDeviceInfoByKey(Long.parseLong(deviceInfo.getServerRegistrationId()));

        if (null == queryDevice) {
            return null;
        }

        queryDevice.setDeviceRegistrationId(deviceInfo.getDeviceRegistrationId());

        Boolean transactionResult = ofy().transact(new Work<Boolean>() {
            @Override
            public Boolean run() {
                ofy().save().entity(queryDevice).now();
                return true;
            }
        });

        return queryDevice;
    }

    /**
     * This method removes the entity with primary key id. It uses HTTP DELETE
     * method.
     * 
     * @param id the primary key of the entity to be deleted.
     * @return The deleted entity.
     */
    @ApiMethod(name = "devices.delete", httpMethod = HttpMethod.DELETE, path = "devices/{id}")
    public DeviceInfo removeDeviceInfo(@Named("id") String id) {

        final DeviceInfo queryDevice = DbHelper.findDeviceInfoByKey(Long.parseLong(id));

        if (null == queryDevice) {
            return null;
        }

        Boolean transactionResult = ofy().transact(new Work<Boolean>() {
            @Override
            public Boolean run() {
                ofy().delete().entity(queryDevice).now();
                return true;
            }
        });

        return queryDevice;
    }

}
