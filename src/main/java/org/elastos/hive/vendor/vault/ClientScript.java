package org.elastos.hive.vendor.vault;

import com.fasterxml.jackson.databind.JsonNode;

import org.elastos.hive.Scripting;
import org.elastos.hive.exception.HiveException;
import org.elastos.hive.scripting.Condition;
import org.elastos.hive.scripting.Executable;
import org.elastos.hive.utils.JsonUtil;
import org.elastos.hive.vendor.connection.ConnectionManager;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;

import okhttp3.MediaType;
import okhttp3.RequestBody;
import retrofit2.Response;

class ClientScript implements Scripting {

    private VaultAuthHelper authHelper;

    ClientScript(VaultAuthHelper authHelper) {
        this.authHelper = authHelper;
    }

    @Override
    public CompletableFuture<Boolean> registerCondition(String name, Condition condition) throws HiveException {
        return authHelper.checkValid()
                .thenCompose(result -> registerConditionImp(name, condition));
    }

    private CompletableFuture<Boolean> registerConditionImp(String name, Condition condition) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                Map map = new HashMap<>();
                map.put("conditionName", name);
                map.put("condition", condition);
                String json = JsonUtil.getJsonFromObject(map);

                Response response = ConnectionManager.getHiveVaultApi()
                        .registerCondition(RequestBody.create(MediaType.parse("Content-Type, application/json"), json))
                        .execute();
                int responseCode = checkResponseCode(response);
                if (responseCode == 404) {
                    throw new HiveException(HiveException.ITEM_NOT_FOUND);
                } else if (responseCode != 0) {
                    throw new HiveException(HiveException.ERROR);
                }
                return true;
            } catch (Exception e) {
                HiveException exception = new HiveException(e.getLocalizedMessage());
                throw new CompletionException(exception);
            }
        });
    }

    @Override
    public CompletableFuture<Boolean> registerScript(String name, Executable executable) throws HiveException {
        return this.registerScript(name, null, executable);
    }

    @Override
    public CompletableFuture<Boolean> registerScript(String name, Condition accessCondition, Executable executable) throws HiveException {
        return authHelper.checkValid()
                .thenCompose(result -> registerScriptImp(name, accessCondition, executable));
    }

    private CompletableFuture<Boolean> registerScriptImp(String name, Condition accessCondition, Executable executable) {
        return CompletableFuture.supplyAsync(() -> {
            try {

                Map map = new HashMap<>();
                map.put("scriptName", name);
                if (accessCondition != null)
                    map.put("accessCondition", accessCondition);
                map.put("executable", executable);

                String json = JsonUtil.getJsonFromObject(map);

                Response response = ConnectionManager.getHiveVaultApi()
                        .registerScript(RequestBody.create(MediaType.parse("Content-Type, application/json"), json))
                        .execute();
                int responseCode = checkResponseCode(response);
                if (responseCode == 404) {
                    throw new HiveException(HiveException.ITEM_NOT_FOUND);
                } else if (responseCode != 0) {
                    throw new HiveException(HiveException.ERROR);
                }
                return true;
            } catch (Exception e) {
                HiveException exception = new HiveException(e.getLocalizedMessage());
                throw new CompletionException(exception);
            }
        });
    }

    @Override
    public <T> CompletableFuture<T> call(String scriptName, Class<T> clazz) {
        return this.call(scriptName, null, clazz);
    }

    @Override
    public <T> CompletableFuture<T> call(String scriptName, JsonNode params, Class<T> clazz) {
        return authHelper.checkValid()
                .thenCompose(result -> callImp(scriptName, params, clazz));
    }

    private <T> CompletableFuture<T> callImp(String scriptName, JsonNode params, Class<T> clazz) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                Map map = new HashMap<>();
                map.put("scriptName", scriptName);
                if (params != null)
                    map.put("params", params);

                String json = JsonUtil.getJsonFromObject(map);


                Response response = ConnectionManager.getHiveVaultApi()
                        .callScript(RequestBody.create(MediaType.parse("Content-Type, application/json"), json))
                        .execute();
                int responseCode = checkResponseCode(response);
                if (responseCode == 404) {
                    throw new HiveException(HiveException.ITEM_NOT_FOUND);
                } else if (responseCode != 0) {
                    throw new HiveException(HiveException.ERROR);
                }

                // TODO: CHECKME!!!
                // TODO: should support String, JsonNode, byte[], Reader
            	// return ResponseHelper.writeToReader(response);
               return null;
            } catch (Exception e) {
                HiveException exception = new HiveException(e.getLocalizedMessage());
                throw new CompletionException(exception);
            }
        });
    }

    private int checkResponseCode(Response response) {
        if (response == null)
            return -1;

        int code = response.code();
        if (code < 300 && code >= 200)
            return 0;

        return code;
    }

}