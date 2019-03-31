package poe.Item.ApiDeserializers;

import com.google.gson.annotations.SerializedName;

import java.util.List;

/**
 * Stash object
 */
public class Stash {
    @SerializedName("stash")
    public String stashName;

    public String id, accountName, stashType, lastCharacterName, league;
    public List<ApiItem> items;

    @SerializedName("public")
    public boolean isPublic;
}
