from os import mkdir, listdir


"""
Input: place outside of folder "database" that contains files named "Bestiary.csv" and the like.
Output: creates folder "out-0001", were it will place folders "Bestiary", where it will place files "accessories.csv",
    which will have their category and key replaced with the index
    
Input format:
    "accessories:belt|Feastbind:Rustic Sash|3::count:15492,inc:3,dec:3,multiplier:0.49,index:06e1-00,quantity:134"
Output format:
    "06e1-00::count:15492,inc:3,dec:3,multiplier:0.49,quantity:134"
"""


def init():
    mkdir("./out-0001")
    input_file_list = listdir("./database")

    for input_file in input_file_list:
        open_file_list = {}
        league = input_file.split(".")[0]
        mkdir("./out-0001/" + league)

        with open("./database/" + input_file, "r") as file:
            for line in file:
                split_line = line.split("::")
                category = split_line[0].split("|", 1)[0].split(":")[0]
                data = split_line[1]

                split_data = data.split(",")
                for var in split_data:
                    mod = var.split(":")[0]
                    val = var.split(":")[1]

                    if mod == "index":
                        split_data.remove(var)
                        split_line[0] = val
                        break

                split_data_joined = ",".join(split_data)
                split_line[1] = split_data_joined
                split_line_joined = "::".join(split_line)

                if category not in open_file_list:
                    open_file_list[category] = open("./out-0001/" + league + "/" + category + ".csv", "w")

                open_file_list[category].write(split_line_joined)

        for category, file in open_file_list.items():
            file.flush()
            file.close()


if __name__ == "__main__":
    init()
