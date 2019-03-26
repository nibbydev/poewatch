package poe.Item.Category;

public enum GroupEnum {
    amulet("amulet", "Amulets"),
    belt("belt", "Belts"),
    ring("ring", "Rings"),

    boots("boots", "Boots"),
    chest("chest", "Body Armours"),
    gloves("gloves", "Gloves"),
    helmet("helmet", "Helmets"),
    quiver("quiver", "Quivers"),
    shield("shield", "Shields"),

    card("card", "Divination cards"),

    currency("currency", "Currency"),
    essence("essence", "Essences"),
    piece("piece", "Harbinger pieces"),
    fossil("fossil", "Fossils"),
    resonator("resonator", "Resonators"),
    vial("vial", "Vials"),

    flask("flask", "Flasks"),

    skill("skill", "Skill Gems"),
    support("support", "Support Gems"),
    vaal("vaal", "Vaal Gems"),

    jewel("jewel", "Jewels"),

    map("map", "Regular Maps"),
    fragment("fragment", "Fragments"),
    unique("unique", "Unique Maps"),
    scarab("scarab", "Scarabs"),

    prophecy("prophecy", "Prophecies"),

    bow("bow", "Bows"),
    claw("claw", "Claws"),
    dagger("dagger", "Daggers"),
    oneaxe("oneaxe", "1H Axes"),
    onemace("onemace", "1H Maces"),
    onesword("onesword", "1H Swords"),
    rod("rod", "Rods"),
    sceptre("sceptre", "Sceptres"),
    staff("staff", "Staves"),
    twoaxe("twoaxe", "2H Axes"),
    twomace("twomace", "2H Maces"),
    twosword("twosword", "2H Swords"),
    wand("wand", "Wands");

    private String name, display;

    GroupEnum(String name, String display) {
        this.name = name;
        this.display = display;
    }

    public String getName() {
        return name;
    }
}
