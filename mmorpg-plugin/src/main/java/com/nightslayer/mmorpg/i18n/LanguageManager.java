package com.nightslayer.mmorpg.i18n;

import com.nightslayer.mmorpg.MMORPGPlugin;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;

/**
 * Manages language files and translations for the plugin.
 */
public class LanguageManager {
    
    private final MMORPGPlugin plugin;
    private final Map<String, FileConfiguration> languages;
    private String defaultLanguage;
    private FileConfiguration currentLang;
    
    public LanguageManager(MMORPGPlugin plugin) {
        this.plugin = plugin;
        this.languages = new HashMap<>();
        this.defaultLanguage = "es_ES";
    }
    
    /**
     * Load language files.
     */
    public void loadLanguages() {
        // Create lang directory if it doesn't exist
        File langDir = new File(plugin.getDataFolder(), "lang");
        if (!langDir.exists()) {
            langDir.mkdirs();
        }
        
        // Load default languages from resources
        saveDefaultLanguage("es_ES");
        saveDefaultLanguage("en_US");
        
        // Load all language files from disk
        File[] langFiles = langDir.listFiles((dir, name) -> name.endsWith(".yml"));
        if (langFiles != null) {
            for (File file : langFiles) {
                String langCode = file.getName().replace(".yml", "");
                FileConfiguration config = YamlConfiguration.loadConfiguration(file);
                languages.put(langCode, config);
                plugin.getLogger().info("Loaded language: " + langCode);
            }
        }
        
        // Set current language from config
        defaultLanguage = plugin.getConfig().getString("language", "es_ES");
        currentLang = languages.getOrDefault(defaultLanguage, languages.get("es_ES"));
        
        if (currentLang == null) {
            plugin.getLogger().severe("No language files loaded!");
        } else {
            plugin.getLogger().info("Using language: " + defaultLanguage);
        }
    }
    
    /**
     * Save default language file from resources.
     */
    private void saveDefaultLanguage(String langCode) {
        File langFile = new File(plugin.getDataFolder(), "lang/" + langCode + ".yml");
        
        if (!langFile.exists()) {
            try {
                InputStream resource = plugin.getResource("lang/" + langCode + ".yml");
                if (resource != null) {
                    plugin.saveResource("lang/" + langCode + ".yml", false);
                } else {
                    // Create empty file with basic structure
                    YamlConfiguration config = new YamlConfiguration();
                    config.set("language.name", langCode);
                    config.save(langFile);
                }
            } catch (Exception e) {
                plugin.getLogger().log(Level.WARNING, "Failed to save default language: " + langCode, e);
            }
        }
    }
    
    /**
     * Get a message from the current language file.
     * 
     * @param key Message key
     * @return Translated message
     */
    public String getMessage(String key) {
        if (currentLang == null) {
            return key;
        }
        
        String message = currentLang.getString(key);
        if (message == null) {
            // Try fallback to es_ES
            FileConfiguration fallback = languages.get("es_ES");
            if (fallback != null) {
                message = fallback.getString(key);
            }
            if (message == null) {
                return key;
            }
        }
        
        return translateColors(message);
    }
    
    /**
     * Get a message with placeholders replaced.
     * 
     * @param key Message key
     * @param placeholders Placeholder replacements (key1, value1, key2, value2, ...)
     * @return Translated message with placeholders replaced
     */
    public String getMessage(String key, Object... placeholders) {
        String message = getMessage(key);
        
        for (int i = 0; i < placeholders.length - 1; i += 2) {
            String placeholder = placeholders[i].toString();
            String value = placeholders[i + 1].toString();
            message = message.replace("{" + placeholder + "}", value);
        }
        
        return message;
    }
    
    /**
     * Translate color codes (&) to Minecraft color codes (§).
     * 
     * @param text Text with & color codes
     * @return Text with § color codes
     */
    private String translateColors(String text) {
        return text.replace('&', '§');
    }
    
    /**
     * Get current language code.
     * 
     * @return Language code (e.g., "es_ES")
     */
    public String getCurrentLanguage() {
        return defaultLanguage;
    }
    
    /**
     * Set current language.
     * 
     * @param langCode Language code
     * @return true if language was set successfully
     */
    public boolean setLanguage(String langCode) {
        FileConfiguration lang = languages.get(langCode);
        if (lang != null) {
            currentLang = lang;
            defaultLanguage = langCode;
            plugin.getConfig().set("language", langCode);
            plugin.saveConfig();
            return true;
        }
        return false;
    }
}
