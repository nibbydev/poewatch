package poe.Item;

public class ItemData {
    private int itemId, categotyId, groupId;

    private String name, typeLine, variation;
    private Integer links, gemLevel, gemQuality, gemCorrupted, mapTier, itemLevel;
    private int frameType;

    private Integer maxStackSize;
    private String icon;

    ItemData() {}

    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }

        if (!ItemData.class.isAssignableFrom(obj.getClass())) {
            return false;
        }

        final ItemData other = (ItemData) obj;

        if (this.frameType != other.frameType) {
            return false;
        }

        if (this.name == null ? (other.name != null) : !this.name.equals(other.name)) {
            return false;
        }
        if (this.typeLine == null ? (other.typeLine != null) : !this.typeLine.equals(other.typeLine)) {
            return false;
        }
        if (this.variation == null ? (other.variation != null) : !this.variation.equals(other.variation)) {
            return false;
        }
        if (this.links == null ? (other.links != null) : !this.links.equals(other.links)) {
            return false;
        }
        if (this.gemLevel == null ? (other.gemLevel != null) : !this.gemLevel.equals(other.gemLevel)) {
            return false;
        }
        if (this.gemQuality == null ? (other.gemQuality != null) : !this.gemQuality.equals(other.gemQuality)) {
            return false;
        }
        if (this.mapTier == null ? (other.mapTier != null) : !this.mapTier.equals(other.mapTier)) {
            return false;
        }
        if (this.gemCorrupted == null ? (other.gemCorrupted != null) : !this.gemCorrupted.equals(other.gemCorrupted)) {
            return false;
        }
        return this.itemLevel == null ? (other.itemLevel == null) : this.itemLevel.equals(other.itemLevel);
    }

    @Override
    public int hashCode() {
        int hash = 3;

        hash = 53 * hash + (this.name != null ? this.name.hashCode() : 0);
        hash = 53 * hash + (this.typeLine != null ? this.typeLine.hashCode() : 0);
        hash = 53 * hash + (this.variation != null ? this.variation.hashCode() : 0);
        hash = 53 * hash + (this.links != null ? this.links.hashCode() : 0);
        hash = 53 * hash + (this.gemLevel != null ? this.gemLevel.hashCode() : 0);
        hash = 53 * hash + (this.gemQuality != null ? this.gemQuality.hashCode() : 0);
        hash = 53 * hash + (this.mapTier != null ? this.mapTier.hashCode() : 0);
        hash = 53 * hash + (this.gemCorrupted != null ? this.gemCorrupted.hashCode() : 0);
        hash = 53 * hash + (this.itemLevel != null ? this.itemLevel.hashCode() : 0);
        hash = 53 * hash + this.frameType;

        return hash;
    }

    @Override
    public String toString() {
        return "name:" + name + "|type:" + typeLine + "|frame:" + frameType + "|ilvl:" + itemLevel + "|links:" + links +
                "|tier:" + mapTier + "|var:" + variation + "|lvl:" + gemLevel + "|qual:" + gemQuality + "|corr:" + gemCorrupted;
    }

    //------------------------------------------------------------------------------------------------------------
    // Getters and Setters
    //------------------------------------------------------------------------------------------------------------

    public String getName() {
        return name;
    }

    public String getTypeLine() {
        return typeLine;
    }

    public Integer getMapTier() {
        return mapTier;
    }

    public Integer getGemQuality() {
        return gemQuality;
    }

    public Integer getGemLevel() {
        return gemLevel;
    }

    public Integer getLinks() {
        return links;
    }

    public String getVariation() {
        return variation;
    }

    public int getFrameType() {
        return frameType;
    }

    public Integer getGemCorrupted() {
        return gemCorrupted;
    }

    public Integer getItemLevel() {
        return itemLevel;
    }

    public Integer getMaxStackSize() {
        return maxStackSize;
    }

    public String getIcon() {
        return icon;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setTypeLine(String typeLine) {
        this.typeLine = typeLine;
    }

    public void setVariation(String variation) {
        this.variation = variation;
    }

    public void setLinks(Integer links) {
        this.links = links;
    }

    public void setGemLevel(Integer gemLevel) {
        this.gemLevel = gemLevel;
    }

    public void setGemQuality(Integer gemQuality) {
        this.gemQuality = gemQuality;
    }

    public void setGemCorrupted(Integer gemCorrupted) {
        this.gemCorrupted = gemCorrupted;
    }

    public void setMapTier(Integer mapTier) {
        this.mapTier = mapTier;
    }

    public void setItemLevel(Integer itemLevel) {
        this.itemLevel = itemLevel;
    }

    public void setFrameType(int frameType) {
        this.frameType = frameType;
    }

    public void setMaxStackSize(Integer maxStackSize) {
        this.maxStackSize = maxStackSize;
    }

    public void setIcon(String icon) {
        this.icon = icon;
    }
}
