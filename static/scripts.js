const shorten = (text, length) => {
    if(text.length <= length) return text;

    return text.substring(0, length - 3) + "...";
}

const isValidString = (text) => {
    return text.length > 3
}

// TODO remove old frontend code
const createArticle = (cluster, rowType, bootstrapColumns, showImage) => {
    let article = cluster.representative

    let a = document.createElement("a")
    a.setAttribute("href", encodeURI(article.url))
    a.className = "article"

    let hlType = "h2"
    if(bootstrapColumns >= 8){
        hlType = "h2"
    } else if (bootstrapColumns >= 4){
        hlType = "h3"
    } else {
        hlType = "h4"
    }

    if (showImage && article.details != undefined && isValidString(article.details.image)) {
        let details = document.createElement("div")
        details.className = "headlineImage"
        details.style = "background-image: url('" + encodeURI(article.details.image) + "');"
        if(article.details.imageCenter != undefined){
            var x = (article.details.imageCenter.x * 100).toFixed()
            var y = (article.details.imageCenter.y * 100).toFixed()
            details.style.cssText += "background-position-x: " + x + "%;"
            details.style.cssText += "background-position-y: " + y + "%;"
        } else {
            console.log("imageCenter not found " + article.details)
        }
        a.appendChild(details)
    }

    // Headline
    let hl = document.createElement(hlType)

    let titleText = article.header
    if(article.details != undefined && isValidString(article.details.title)) {
        titleText = article.details.title
    }

    hl.appendChild(document.createTextNode(shorten(titleText, 120)))
    a.appendChild(hl)

    if(rowType.description && article.details != undefined){
        let details = document.createElement("div")
        details.className = "description"

        details.appendChild(
            document.createTextNode(
                shorten(article.details.description, bootstrapColumns >= 8 ? 300 : 150)
            )
        )

        a.appendChild(details)
    }

    // Source
    let sourceSpan = document.createElement("span")

    let source = document.createElement("a")
    source.setAttribute("href", encodeURI(article.url))
    source.className = "sourceLink"
    source.appendChild(document.createTextNode(article.source))
    sourceSpan.appendChild(source)

    // More
    let num = document.createElement("span")
    num.appendChild(document.createTextNode(" +" + (cluster.articles.length - 1)))
    sourceSpan.appendChild(num)

    a.appendChild(sourceSpan)

    return a
}

const createColumn = (clusters, rowType, index) => {
    let columnDiv = document.createElement("div");

    bootstrapColumns = rowType.bootstrapColumns[index]
    columnDiv.className = "col-sm-" + bootstrapColumns

    for(let i = 0; clusters.length > 0 && i < rowType.articleCount[index]; ++i){
        let a = createArticle(clusters[0], rowType, bootstrapColumns, rowType.image[index])
        columnDiv.appendChild(a)
        clusters.shift()
    }

    return columnDiv
}

const addRow = (clusters, index) => {
    let rowDiv = document.createElement("div");
    rowDiv.className = "row"

    rowTypes = [
        {columns:2, description:true, bootstrapColumns:[8, 4], image:[true, false], articleCount:[1, 2]},
        {columns:3, description:true, bootstrapColumns:[4, 4, 4], image:[true, true, true], articleCount:[1, 1, 1]},
        {columns:4, description:true, bootstrapColumns:[3, 3, 3, 3], image:[true, true, true, true], articleCount:[1, 1, 1, 1]},
        {columns:2, description:true, bootstrapColumns:[4, 8], image:[false, true], articleCount:[2, 1]},
        {columns:3, description:true, bootstrapColumns:[4, 4, 4], image:[true, true, true], articleCount:[1, 1, 1]},
        {columns:4, description:true, bootstrapColumns:[3, 3, 3, 3], image:[true, true, true, true], articleCount:[1, 1, 1, 1]},
    ]

    rowType = rowTypes[index % rowTypes.length]
    for(let i = 0; clusters.length > 0 && i < rowType.columns; ++i) {
        rowDiv.appendChild(createColumn(clusters, rowType, i))
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

//requestArticles()