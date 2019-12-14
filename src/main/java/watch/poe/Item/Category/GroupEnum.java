package poe.Item.Category;

public enum GroupEnum {
    amulet,
    belt,
    ring,

    boots,
    chest,
    gloves,
    helmet,
    quiver,
    shield,

    card,

    currency,
    essence,
    piece,
    fossil,
    resonator, 
    vial,
    net,

    flask,

    skill,
    support, 
    vaal,

    jewel,

    map,
    fragment,
    unique,
    scarab,

    prophecy,

    bow,
    claw,
    dagger, 
    oneaxe,
    onemace,
    onesword,
    rod,
    sceptre, 
    staff,
    twoaxe,
    twomace,
    twosword,
    wand, 
    incubator,
    splinter,
    runedagger,
    warstaff,
    oil,
    beast,
    sample,
    catalyst;

    public int getId() {return ordinal() + 1;}
}
