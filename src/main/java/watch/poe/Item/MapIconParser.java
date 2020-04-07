package poe.Item;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import poe.League.LeagueManager;

import java.util.Base64;
import java.util.Map;
import java.util.Optional;

public class MapIconParser {
  private static final Logger log = LoggerFactory.getLogger(LeagueManager.class);
  private static final Gson gson = new Gson();

  public static Integer parseSeries(String icon) {
    if (icon == null) {
      return null;
    }

    // [28,14,{"f":"2DItems\/Maps\/Atlas2Maps\/New\/Museum","w":1,"h":1,"scale":true,"mn":7,"mt":3}]
    Map<String, String> params;
    try {
      String json = extractBase64(icon);
      params = extractParams(json);
    } catch (Exception ex) {
      log.error("Couldn't parse map tier", ex);
      return null;
    }

    return parseMapSeries(params);
  }

  private static String extractBase64(String icon) {
    // https://web.poecdn.com/gen/image/WzI4LDE0LHsiZiI6IjJESXRlbXNcL01hcHNcL0F0bGFzMk1hcHNcL05ld1wvTXVzZXVtIiwidyI6MSwiaCI6MSwic2NhbGUiOnRydWUsIm1uIjo3LCJtdCI6M31d/fb4a9b4077/Item.png
    String[] tmp = icon.split("/");
    String base64 = tmp[tmp.length - 3];
    byte[] bytes = Base64.getDecoder().decode(base64);
    return new String(bytes);
  }

  private static Map<String, String> extractParams(String base64) {
    String paramJson = base64.substring(base64.indexOf('{'), base64.indexOf('}') + 1);
    return gson.fromJson(base64, new TypeToken<Map<String, String>>() {
    }.getType());
  }

  private static Integer parseMapSeries(Map<String, String> params) throws RuntimeException {
    /* Currently the series are as such:
     todo: update outdated links
     http://web.poecdn.com/image/Art/2DItems/Maps/Map45.png?scale=1&w=1&h=1
     http://web.poecdn.com/image/Art/2DItems/Maps/act4maps/Map76.png?scale=1&w=1&h=1
     http://web.poecdn.com/image/Art/2DItems/Maps/AtlasMaps/Chimera.png?scale=1&scaleIndex=0&w=1&h=1
     http://web.poecdn.com/image/Art/2DItems/Maps/Atlas2Maps/New/VaalTempleBase.png?scale=1&w=1&h=1&mn=1&mt=0
     http://web.poecdn.com/image/Art/2DItems/Maps/Atlas2Maps/New/VaalTempleBase.png?scale=1&w=1&h=1&mn=2&mt=0
     http://web.poecdn.com/image/Art/2DItems/Maps/Atlas2Maps/New/VaalTempleBase.png?scale=1&w=1&h=1&mn=3&mt=0
    */

    String iconCategory = parseIconCategory(params);
    Optional<Integer> seriesNumber = parseSeriesParam(params);

    if (iconCategory.equalsIgnoreCase("Maps")) {
      return 0;
    } else if (iconCategory.equalsIgnoreCase("act4maps")) {
      return 1;
    } else if (iconCategory.equalsIgnoreCase("AtlasMaps")) {
      return 2;
    } else if (iconCategory.equalsIgnoreCase("New") && seriesNumber.isPresent()) {
      return seriesNumber.get() + 2;
    } else {
      throw new RuntimeException("DiscardErrorCode.INVALID_MAP_SERIES");
    }
  }

  private static String parseIconCategory(Map<String, String> paramMap) throws RuntimeException {
    if (paramMap == null || !paramMap.containsKey("f")) {
      throw new RuntimeException("ParseErrorCode.PARSE_MAP_ICON_PARAM_F");
    }

    String[] split = paramMap.get("f").split("/");
    return split[split.length - 2].toLowerCase();
  }

  private static Optional<Integer> parseSeriesParam(Map<String, String> paramMap) throws RuntimeException {
    // older maps will not have this param
    if (!paramMap.containsKey("mn")) {
      return Optional.empty();
    }

    try {
      Integer series = Integer.parseInt(paramMap.get("mn"));
      return Optional.of(series);
    } catch (Exception ex) {
      throw new RuntimeException(", ParseErrorCode.PARSE_MAP_ICON_PARAM_MN", ex);
    }
  }

}
