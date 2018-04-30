from os import mkdir
import json as jayson

"""
Input: place in same folder as "itemData.json"
Output: creates folder "out-0002", were it will place file "itemData.json" which will have its subIndexes categories 
    removed
"""


def init():
    mkdir("./out-0002")

    with open("itemData.json", "r") as inputFile:
        json = jayson.loads(inputFile.read())

        for super_index, super_item in json.items():
            for sub_index, sub_item in super_item["subIndexes"].items():
                sub_item["specificKey"] = sub_item["specificKey"].split("|", 1)[1]

    with open("./out-0002/itemData.json", "w") as outputFile:
        outputFile.write(jayson.dumps(json))


if __name__ == "__main__":
    init()
