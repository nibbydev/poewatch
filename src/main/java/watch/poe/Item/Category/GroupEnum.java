package poe.Item.Category;

public enum GroupEnum {
    amulet("amulet", "Amulets"),
    belt("belt", "Belts"),
    ring("ring", "Rings"),

    boots("boots", "Boots"),
    chest("chest", "Body Armours"), //5
    gloves("gloves", "Gloves"),
    helmet("helmet", "Helmets"),
    quiver("quiver", "Quivers"),
    shield("shield", "Shields"),

    card("card", "Divination cards"), //10

    currency("currency", "Currency"),
    essence("essence", "Essences"),
    piece("piece", "Harbinger pieces"),
    fossil("fossil", "Fossils"),
    resonator("resonator", "Resonators"), //15
    vial("vial", "Vials"),
    net("net", "Nets"),

    flask("flask", "Flasks"),

    skill("skill", "Skill Gems"),
    support("support", "Support Gems"), //20
    vaal("vaal", "Vaal Gems"),

    jewel("jewel", "Jewels"),

    map("map", "Regular Maps"),
    fragment("fragment", "Fragments"),
    unique("unique", "Unique Maps"), //25
    scarab("scarab", "Scarabs"),

    prophecy("prophecy", "Prophecies"),

    bow("bow", "Bows"),
    claw("claw", "Claws"),
    dagger("dagger", "Daggers"), //30
    oneaxe("oneaxe", "1H Axes"),
    onemace("onemace", "1H Maces"),
    onesword("onesword", "1H Swords"),
    rod("rod", "Rods"),
    sceptre("sceptre", "Sceptres"), //35
    staff("staff", "Staves"),
    twoaxe("twoaxe", "2H Axes"),
    twomace("twomace", "2H Maces"),
    twosword("twosword", "2H Swords"),
    wand("wand", "Wands"); //40

    private String name, display;

    GroupEnum(String name, String display) {
        this.name = name;
        this.display = display;
    }

    public String getName() {
        return name;
    }

    public String getDisplay() {
        return display;
    }

    public int getId() {return ordinal() + 1;}
}
