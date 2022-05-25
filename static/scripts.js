const shorten = (text, length) => {
    if(text.length <= length) return text;

    return text.substring(0, length - 3) + "...";
}

const isValidString = (text) => {
    return text.length > 3
}

const createColumn = (cluster, bootstrapColumns) => {
    let columnDiv = document.createElement("div");
    columnDiv.className = "col-sm-" + bootstrapColumns

    let article = cluster.representative

    let hlType = "h2"
    if(bootstrapColumns >= 8){
        hlType = "h2"
    } else if (bootstrapColumns >= 4){
        hlType = "h3"
    } else {
        hlType = "h4"
    }

    if (bootstrapColumns == 3 && article.details != undefined && isValidString(article.details.image)) {
        let details = document.createElement("div")
        details.className = "headlineImage"
        details.style = "background-image: url('" + encodeURI(article.details.image) + "')"

        console.log(article.details.title)
        console.log(article.details.image)

        columnDiv.appendChild(details)
    }

    // Headline
    let hl = document.createElement(hlType)

    let titleText = article.header
    if(article.details != undefined && isValidString(article.details.title)) {
        titleText = article.details.title
    }

    hl.appendChild(document.createTextNode(titleText))
    columnDiv.appendChild(hl)

    if(bootstrapColumns >= 4 && article.details != undefined){
        let details = document.createElement("div")
        details.className = "description"

        details.appendChild(
            document.createTextNode(
                shorten(article.details.description, bootstrapColumns >= 8 ? 300 : 150)
            )
        )

        columnDiv.appendChild(details)
    }

    // Source
    let sourceSpan = document.createElement("span")

    let a = document.createElement("a")
    a.setAttribute("href", encodeURI(article.url))
    a.appendChild(document.createTextNode(article.source))
    sourceSpan.appendChild(a)

    // More
    let num = document.createElement("span")
    num.appendChild(document.createTextNode(" +" + (cluster.articles.length - 1)))
    sourceSpan.appendChild(num)

    columnDiv.appendChild(sourceSpan)

    return columnDiv
}

const addRow = (clusters, index) => {
    let rowDiv = document.createElement("div");
    rowDiv.className = "row"

    numColumnsTable = [2, 3, 4, 2, 3, 4, 2, 3, 4, 2, 3, 4, 2]
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

    result = result.reverse()

    let i = 0
    while(result.length > 0){
        addRow(result, i++)
    }
}

requestArticles()