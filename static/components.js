'use strict';

const e = React.createElement;

function orDefault(value, defaultValue){
    if(value != undefined || value != null) {
        return value
    } else {
        return defaultValue
    }
}

const shorten = (text, length) => {
    if(text.length <= length) return text;

    return text.substring(0, length - 3) + "...";
}

const isValidString = (text) => {
    return text.length > 3
}

// TODO: Kinds of articles
// Large image with Title on top, followed by a short description 3 width
// Same thing with 1 width

class ConfigurableArticleTile extends React.Component {
    constructor(props) {
        super(props)

        this.config = {
            maxDescriptionLength: orDefault(this.props.maxDescriptionLength, 300),
            showDescription: orDefault(this.props.showDescription, true),
            maxTitleLength: orDefault(this.props.maxTitleLength, 150),
            headlineType: orDefault(this.props.headlineType, 'h3'), // tested with h2/h3/h4
            showImage: orDefault(this.props.showImage, true),
            bootstrapColumnType: orDefault(this.props.bootstrapColumnType, "col-sm-3"), // tested with 8/3/4
            articleWrapperClassNames: orDefault(this.props.articleWrapperClassNames, []),
            afterArticleClassNames: orDefault(this.props.afterArticleClassNames, [])
        }
    }

    getWidth() {
        let total = 12.0 // bootstraps total width
        let segments = this.config.bootstrapColumnType.split('-')
        let last = segments[segments.length - 1]
        return Number.parseFloat(last) / total
    }
}

function renderArticle(article, config, autoHeight) {
    let uid = 0

    let rep = article.representative

    if(rep == undefined){
        return e('a', {href: "NOTHING", className: "article"}) // TODO
    }

    let details = rep.details
    console.log(rep)

    if(details == null) {
        details = {title: "", description: "", image: "", imageCenter: {x: 0.5, y: 0.5}}
    }

    if(details.imageCenter == null) {
        details.imageCenter = {x: 0.5, y: 0.5}
    }

    let x = (details.imageCenter.x * 100).toFixed()
    let y = (details.imageCenter.y * 100).toFixed()

    // TODO: Show summary
    // TODO: Show list of articles (more...)

    let elements = []

    // Image
    if(config.showImage) {
        elements.push(e('div', {key: uid++, className: "headlineImage",
            style: {
                backgroundImage: "url('"+details.image+"')", // encodeURI
                backgroundPositionX: x + "%",
                backgroundPositionY: y + "%"
            }}))
    }

    let afterImageElements = []

    // Headline
    let title = rep.header
    if(details.title.length > 0 && title.length > details.title){
        title = details.title
    }
    afterImageElements.push(e(config.headlineType, {key: uid++}, shorten(title, config.maxTitleLength)))

    if(config.showDescription) {
        // Description
        afterImageElements.push(e('div', {key: uid++, className: "description"}, shorten(details.description, config.maxDescriptionLength)))
    }

    // Sources
    afterImageElements.push(e('span', {key: uid++, className: "sourceLink"},
        e('span', {key: uid++}, rep.source),
        e('span', {key: uid++}, " +" + (article.articles.length - 1))
    ))

    let afterArticleClassNames = ["afterArticle"].concat(config.afterArticleClassNames)
    elements.push(e('div', {className: afterArticleClassNames.join(" ")}, afterImageElements))
    let a = e('a', {href: rep.url, className: "article"}, elements)

    let articleWrapperClassNames = ["article_wrapper"].concat(config.articleWrapperClassNames)

    let stl = {}
    if(autoHeight) {
        stl = { height: "auto" }
    }

    return e('div', {className: articleWrapperClassNames.join(" "), style: stl}, a)
}

class MultipleArticlesColumn extends ConfigurableArticleTile {
    constructor(props) {
        super(props)

        this.config.showImage = false
        this.config.headlineType = 'h4'
        this.config.bootstrapColumnType = "col-sm-3"
        this.config.showDescription = true
        this.config.maxDescriptionLength = 120
    }

    render() {
        let articleElements = []
        let articles = this.props.articles
        let uid = 0

        for(let idx in articles){
            articleElements.push(renderArticle(articles[idx], this.config, true))
        }
        return e('div', {key: uid++, className: this.config.bootstrapColumnType}, articleElements)
    }
}

class ArticleTile extends ConfigurableArticleTile {
    constructor(props) {
        super(props)
    }

    render() {
        return e('div', {key: 0, className: this.config.bootstrapColumnType},
            renderArticle(this.props.article, this.config))
    }
}

class ArticleTileTextAndImageSmall extends ArticleTile {
    constructor(props) {
        super(props)
        this.config.articleWrapperClassNames = ["light"]
        // this.config.afterArticleClassNames = ["paddingSmall"]
        // use all default arguments for this.config
    }
}

class ArticleTileTextAndImageSmallNoDescription extends ArticleTile {
    constructor(props) {
        super(props)
        this.config.articleWrapperClassNames = ["light"]
        this.config.showDescription = false
        // use all default arguments for this.config
    }
}

class ArticleTileTextSmall extends ArticleTile {
    constructor(props) {
        super(props)
        this.config.showImage = false
        this.config.headlineType = 'h4'
    }
}

class ArticleTileTextLarge extends ArticleTile {
    constructor(props) {
        super(props)
        this.config.headlineType = 'h2'
        this.config.bootstrapColumnType = 'col-sm-9'
        this.config.articleWrapperClassNames = ["dark"]
        //this.config.afterArticleClassNames = ["padding"]
    }
}

class ArticleTileTextLargeVertical extends ArticleTile {
    constructor(props) {
        super(props)
        this.config.headlineType = 'h2'
        this.config.bootstrapColumnType = 'col-sm-9'
        this.config.articleWrapperClassNames = ["dark", "vertical"]
    }
}

function take(source, n) {
    let result = []
    while(source.length > 0 && result.length < n){
        result.push(source[0])
        source.shift()
    }
    return result
}

function getBool(seed) {
    return seed % 2 == 1;
}

class ArticleRow extends React.Component {
    constructor(props) {
        super(props)

        this.seed = (this.props.index - 1)
        // TODO sort clusters by number of articles
    }

    render() {
        let articles = this.props.articles
        let columns = []
        let uid = 0

        if(articles.length == 2) {
            if(getBool(this.seed)) {
                columns.push(e(ArticleTileTextAndImageSmall, {key: uid++, article: articles[1]}))
                columns.push(e(ArticleTileTextLargeVertical, {key: uid++, article: articles[0]}))
            } else {
                columns.push(e(ArticleTileTextLargeVertical, {key: uid++, article: articles[0]}))
                columns.push(e(ArticleTileTextAndImageSmall, {key: uid++, article: articles[1]}))
            }
        } else if(articles.length == 3) {
            if(getBool(this.seed)) {
                columns.push(e(ArticleTileTextLarge, {key: uid++, article: articles[0]}))
                columns.push(e(MultipleArticlesColumn, {key: uid++, articles: [articles[1], articles[2]]}))
            } else {
                columns.push(e(MultipleArticlesColumn, {key: uid++, articles: [articles[1], articles[2]]}))
                columns.push(e(ArticleTileTextLarge, {key: uid++, article: articles[0]}))
            }
        } else if(articles.length == 4) {

            let type = ArticleTileTextAndImageSmall
            if(getBool(this.seed)) type = ArticleTileTextAndImageSmallNoDescription

            for(let idx in articles) {
                columns.push(e(type, {key: uid++, article: articles[idx]}))
            }
        }

        // TODO: Three same small articles
        // TODO: One article?
        // TODO: Check if any has no description
        // TODO: Large image article
        // TODO: List of article headlines
        // TODO: Centered text

        return e('div', {className: "row"}, columns)
    }
}

class ArticleGrid extends React.Component {
    constructor(props) {
        super(props)
    }

    render() {
        let articles = this.props.articles

        let articleNumberOptions = [2, 4, 3, 4]

        let rows = []
        let uid = 0
        while(articles.length > 4 && rows.length < 200){
            let num = articleNumberOptions[uid % articleNumberOptions.length]
            let element = e(ArticleRow, {key: uid++, articles: take(articles, num), index: uid})
            rows.push(element)
        }

        return e('div', {className: "container"}, rows)
    }
}

const main = async () => {
    const response = await fetch('clusters.json');
    let result = await response.json();
    result = result.reverse()

    const domContainer = document.querySelector('#articles');
    const root = ReactDOM.createRoot(domContainer);
    root.render(e(ArticleGrid, {articles: result}));
}

main()
