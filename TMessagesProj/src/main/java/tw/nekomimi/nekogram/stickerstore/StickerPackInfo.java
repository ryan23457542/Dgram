package tw.nekomimi.nekogram.stickerstore;

import org.json.JSONObject;

public class StickerPackInfo {

    public String shortName;
    public String title;
    public String category;
    public double rating;
    public int downloads;

    public static StickerPackInfo fromJson(JSONObject obj) {
        StickerPackInfo info = new StickerPackInfo();
        info.shortName = firstString(obj, "short_name", "shortName", "name");
        info.title = firstString(obj, "title", "display_name", "displayName");
        if (info.title == null || info.title.isEmpty()) {
            info.title = info.shortName;
        }
        info.category = firstString(obj, "category", "category_name");
        info.rating = firstDouble(obj, "rating", "avg_rating", "average_rating");
        info.downloads = firstInt(obj, "downloads", "download_count", "downloads_count");
        return info;
    }

    private static String firstString(JSONObject obj, String... keys) {
        for (String key : keys) {
            if (obj.has(key) && !obj.isNull(key)) {
                String value = obj.optString(key, null);
                if (value != null && !value.isEmpty()) {
                    return value;
                }
            }
        }
        return null;
    }

    private static double firstDouble(JSONObject obj, String... keys) {
        for (String key : keys) {
            if (obj.has(key) && !obj.isNull(key)) {
                return obj.optDouble(key, 0);
            }
        }
        return 0;
    }

    private static int firstInt(JSONObject obj, String... keys) {
        for (String key : keys) {
            if (obj.has(key) && !obj.isNull(key)) {
                return obj.optInt(key, 0);
            }
        }
        return 0;
    }
}
