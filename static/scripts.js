const createColumn = (cluster, bootstrapColumns) => {
    let columnDiv = document.createElement("div");
    columnDiv.className = "col-sm-" + bootstrapColumns

    // Headline
    let hl = document.createElement("h2")
    hl.appendChild(document.createTextNode(cluster.articles[0].header))
    columnDiv.appendChild(hl)

    // Source
    let a = document.createElement("a")
    a.setAttribute("href", cluster.articles[0].url)
    a.appendChild(document.createTextNode(cluster.articles[0].source))
    columnDiv.appendChild(a)

    return columnDiv
}

const addRow = (clusters, index) => {
    let rowDiv = document.createElement("div");
    rowDiv.className = "row"

    numColumnsTable = [2, 3, 4, 3, 2, 3, 4, 3, 4, 2, 3, 4, 2]
    numColumns = numColumnsTable[index % numColumnsTable.length]

    for(i = 0; i < numColumns && clusters.length > 0; ++i) {
        bootstrapColumns = 12 / numColumns

        // Special logic for the 2 columns case
        if(numColumns === 2){
            if(i == 0) bootstrapColumns = 8
            if(i == 1) bootstrapColumns = 4
        }

        rowDiv.appendChild(createColumn(clusters[0], bootstrapColumns))
        clusters.shift()
    }

    document.getElementById("articles").appendChild(rowDiv)
}

const requestArticles = async () => {
    const response = await fetch('clusters.json');
    let result = await response.json();
    console.log(result)

    result = result.reverse()

    let i = 0
    while(result.length > 0){
        addRow(result, i++)
    }
}

requestArticles()