from os import mkdir


def init():
    mkdir("./out")

    file_list = {}

    with open("database.txt", "r") as inputFile:
        for line in inputFile:
            split_line = line.split("|", 1)
            league = split_line[0]
            data = split_line[1]

            if league not in file_list:
                file_list[league] = open("./out/" + league + ".csv", "w")

            file_list[league].write(data)

    for league, file in file_list.items():
        file.flush()
        file.close()


if __name__ == "__main__":
    init()
