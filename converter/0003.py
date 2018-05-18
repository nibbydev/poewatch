from os import mkdir, listdir
from math import floor
import json as jayson


"""
Input: 
    + in-0003/
    |
    +---+ Flashback Event (BRE001)/
    |   + accessories.json
    |   + armour.json
    |   + cards.json
    |   + ...
    |
    +---+ HC Flashback Event (BRE002)/
    |   + accessories.json
    |   + armour.json
    |   + cards.json
    |   + ...
    |
    +---+ Bestiary/
    |   + accessories.json
    |   + armour.json
    |   + cards.json
    |   + ...
    |
    +---+ Hardcore Bestiary/
    |   + accessories.json
    |   + armour.json
    |   + cards.json
    |   + ...
    
Commands Flash:
    (base "cli_all_[16.04.2018][06.00.14]")
    
    0 -> 1
    0 (from "cli_all_[05.04.2018][05.13.38] (after 24h)") -> 0
    
    7 (from "cli_all_[13.04.2018][13.44.33]") -> 7
    
    9 -> 8
    
    11 -> 9
    >= 10 del
    
Commands Bestiary:
    (base "cli_all_[16.04.2018][06.00.14]")
    
    73 -> 71
    74 -> 72

    71 (from "cli_all_[13.04.2018][13.44.33]") -> 70
    
    >= 73 del
"""


def step_one(json, tuples):
    for index, entry in json.items():
        for key, values in entry.items():
            for tuplee in tuples:
                json[index][key][tuplee[0]] = values[tuplee[1]]


def step_two_flash(league, file, json):
    with open("./in-0003/05/" + league + "/" + file, "r") as f:
        json_05 = jayson.loads(f.read())

        for index, entry in json.items():
            for key, values in entry.items():
                if index in json_05:
                    json[index][key][0] = json_05[index][key][0]
                else:
                    json[index][key][0] = 0


def step_three(league, file, json, tuples):
    with open("./in-0003/13/" + league + "/" + file, "r") as f:
        json_13 = jayson.loads(f.read())

        for index, entry in json.items():
            for key, values in entry.items():
                for tuplee in tuples:
                    if index in json_13:
                        json[index][key][tuplee[0]] = json_13[index][key][tuplee[1]]
                    else:
                        json[index][key][tuplee[0]] = 0


def step_four_flash(json):
    for index, entry in json.items():
        for key, values in entry.items():
            json[index][key][8] = values[9]
            json[index][key][10] = values[11]

            if key == "count" or key == "quantity":
                json[index][key][9] = floor((values[9] + values[11]) / 2)
            else:
                json[index][key][9] = round((values[9] + values[11]) / 2, 3)


def step_five(json, limit):
    for index, entry in json.items():
        for key, values in entry.items():
            json[index][key] = values[:limit]


def init():
    mkdir("./out-0003")

    leagues = listdir("./in-0003/current")
    files = listdir("./in-0003/current/" + leagues[0])

    for league in leagues:
        mkdir("./out-0003/" + league)

        for file in files:
            with open("./in-0003/current/" + league + "/" + file, "r") as fi:
                json = jayson.loads(fi.read())

                if "Bestiary" in league:
                    step_one(json, [(71, 73), (72, 74)])
                    step_three(league, file, json, [(70, 71)])
                    step_five(json, 73)
                elif "Event" in league:
                    step_one(json, [(1, 0)])
                    step_two_flash(league, file, json)
                    step_three(league, file, json, [(7, 7)])
                    step_four_flash(json)
                    step_five(json, 11)

                with open("./out-0003/" + league + "/" + file, "w") as fo:
                    fo.write(jayson.dumps(json))


if __name__ == "__main__":
    init()
