package ru.nbk.rolecases.util;

import kong.unirest.HttpResponse;
import kong.unirest.Unirest;
import kong.unirest.UnirestException;
import net.md_5.bungee.api.ChatColor;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.Plugin;
import org.json.JSONObject;
import ru.nbk.rolecases.HolyCases;

//Защита KeyAuth
public class Protect {
    public final String appname;
    public final String ownerid;
    public final String version;
    public final String url;

    protected String sessionid;
    protected boolean initialized;

    public Protect(String appname, String ownerid, String version, String url, HolyCases plugin) {
        this.appname = appname;
        this.ownerid = ownerid;
        this.version = version;
        this.url = url;

        HttpResponse<String> response;
        try {
            response = Unirest.post(url).field("type", "init").field("ver", version).field("name", appname)
                    .field("ownerid", ownerid).asString();

            try {
                JSONObject responseJSON = new JSONObject(response.getBody());

                if (response.getBody().equalsIgnoreCase("KeyAuth_Invalid")) {
                    System.out.println("Защита выключена. Проблема на стороне разработчика");
                    throw new HWIDException();
                }

                if (responseJSON.getBoolean("success")) {
                    sessionid = responseJSON.getString("sessionid");
                    initialized = true;
                    plugin.getLogger().info(ChatColor.GREEN + "Session ID: " + responseJSON.getString("sessionid"));
                } else {
                    plugin.getLogger().severe("Ошибка подключения: " + responseJSON.getString("message"));
                    throw new HWIDException();
                }

            } catch (Exception e) {
                plugin.getLogger().severe("Ошибка подключения");
                throw new HWIDException();
            }
        } catch (UnirestException e) {
            plugin.getLogger().severe("Ошибка подключения");
            throw new HWIDException();
        }

        try {
            String hwid = HWID.getHWID();

            response = Unirest.post(url).field("type", "login").field("username", plugin.getConfig().getString("protect.username"))
                    .field("pass", plugin.getConfig().getString("protect.password"))
                    .field("hwid", hwid).field("sessionid", sessionid).field("name", appname).field("ownerid", ownerid)
                    .asString();

            try {
                JSONObject responseJSON = new JSONObject(response.getBody());

                if (!responseJSON.getBoolean("success")) {
                    plugin.getLogger().severe("Вы не имеете доступа к плагину");
                    throw new HWIDException();
                }
            } catch (Exception e) {
                plugin.getLogger().severe("Ошибка доступа");
                throw new HWIDException();
            }
        } catch (UnirestException e) {
            plugin.getLogger().severe("Ошибка доступа");
            throw new HWIDException();
        }

        plugin.getLogger().info(org.bukkit.ChatColor.GREEN + "Доступ разрешен!");
    }
}