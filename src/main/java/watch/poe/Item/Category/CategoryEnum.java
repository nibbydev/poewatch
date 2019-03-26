package poe.Item.Category;

public enum CategoryEnum {
    accessory("accessory", "Accessories",
            GroupEnum.amulet,
            GroupEnum.ring,
            GroupEnum.belt),

    armour("armour", "Armour",
            GroupEnum.boots,
            GroupEnum.chest,
            GroupEnum.gloves,
            GroupEnum.helmet,
            GroupEnum.quiver,
            GroupEnum.shield),

    card("card", "Divination cards",
            GroupEnum.card),

    currency("currency", "Currency",
            GroupEnum.currency,
            GroupEnum.essence,
            GroupEnum.piece,
            GroupEnum.fossil,
            GroupEnum.resonator,
            GroupEnum.vial),

    enchantment("enchantment", "Enchants",
            GroupEnum.helmet,
            GroupEnum.boots,
            GroupEnum.gloves),

    flask("flask", "Flasks",
            GroupEnum.flask),

    gem("gem", "Gems",
            GroupEnum.skill,
            GroupEnum.support,
            GroupEnum.vaal),

    jewel("jewel", "Jewels",
            GroupEnum.jewel),

    map("map", "Maps",
            GroupEnum.map,
            GroupEnum.fragment,
            GroupEnum.unique,
            GroupEnum.scarab),

    prophecy("prophecy", "Prophecy",
            GroupEnum.prophecy),

    weapon("weapon", "Weapons",
            GroupEnum.bow,
            GroupEnum.claw,
            GroupEnum.dagger,
            GroupEnum.oneaxe,
            GroupEnum.onemace,
            GroupEnum.onesword,
            GroupEnum.rod,
            GroupEnum.sceptre,
            GroupEnum.staff,
            GroupEnum.twoaxe,
            GroupEnum.twomace,
            GroupEnum.twosword,
            GroupEnum.wand),

    base("base", "Crafting Bases",
            GroupEnum.amulet,
            GroupEnum.ring,
            GroupEnum.belt,
            GroupEnum.boots,
            GroupEnum.chest,
            GroupEnum.gloves,
            GroupEnum.helmet,
            GroupEnum.quiver,
            GroupEnum.shield,
            GroupEnum.bow,
            GroupEnum.claw,
            GroupEnum.dagger,
            GroupEnum.oneaxe,
            GroupEnum.onemace,
            GroupEnum.onesword,
            GroupEnum.rod,
            GroupEnum.sceptre,
            GroupEnum.staff,
            GroupEnum.twoaxe,
            GroupEnum.twomace,
            GroupEnum.twosword,
            GroupEnum.wand,
            GroupEnum.jewel);

    private String name, display;
    private GroupEnum[] groups;

    CategoryEnum(String name, String display, GroupEnum... groups) {
        this.name = name;
        this.display = display;
        this.groups = groups;
    }

    public GroupEnum[] getGroups() {
        return groups;
    }

    public String getName() {
        return name;
    }
}
