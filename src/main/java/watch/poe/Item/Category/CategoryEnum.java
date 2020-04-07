package poe.Item.Category;

public enum CategoryEnum {
    accessory(
            GroupEnum.amulet,
            GroupEnum.ring,
            GroupEnum.belt
    ),
    armour(
            GroupEnum.boots,
            GroupEnum.chest,
            GroupEnum.gloves,
            GroupEnum.helmet,
            GroupEnum.quiver,
            GroupEnum.shield
    ),
    card(
            GroupEnum.card
    ),
    currency(
            GroupEnum.currency,
            GroupEnum.essence,
            GroupEnum.piece,
            GroupEnum.fossil,
            GroupEnum.resonator,
            GroupEnum.vial,
            GroupEnum.oil,
            GroupEnum.deliriumorb
    ),
    enchantment(
            GroupEnum.helmet,
            GroupEnum.boots,
            GroupEnum.gloves
    ),
    flask(
            GroupEnum.flask
    ),
    gem(
            GroupEnum.skill,
            GroupEnum.support,
            GroupEnum.vaal
    ),
    jewel(
            GroupEnum.jewel
    ),
    map(
            GroupEnum.map,
            GroupEnum.fragment,
            GroupEnum.unique,
            GroupEnum.scarab
    ),
    prophecy(
            GroupEnum.prophecy
    ),
    weapon(
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
            GroupEnum.runedagger,
            GroupEnum.warstaff
    ),
    base(
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
            GroupEnum.jewel,
            GroupEnum.warstaff,
            GroupEnum.runedagger
    ),
    beast(
            GroupEnum.beast
    );

    private final GroupEnum[] groups;

    CategoryEnum(GroupEnum... groups) {
        this.groups = groups;
    }

    public GroupEnum[] getGroups() {
        return groups;
    }


    public int getId() {
        return ordinal() + 1;
    }
}
