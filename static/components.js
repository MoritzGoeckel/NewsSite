'use strict';

const e = React.createElement;

function orDefault(value, defaultValue){
    if(value != undefined || value != null) {
        return value
    } else {
        return defaultValue
    }
}

// TODO: Kinds of articles
// Large image with Title on top, followed by a short description 3 width
// Same thing with 1 width
//

class ConfigurableArticleTile extends React.Component {
    constructor(props) {
        super(props)

        if(this.props.article == undefined && this.props.articles == undefined) {
            throw "props.article / props.articles for ConfigurableArticleTile is required"
        }

        this.config = {
            maxDescriptionLength: orDefault(this.props.maxDescriptionLength, 300),
            maxTitleLength: orDefault(this.props.maxTitleLength, 150),
            headlineType: orDefault(this.props.headlineType, 'h3'), // tested with h2/h3/h4
            showImage: orDefault(this.props.showImage, true),
            bootstrapColumnType: orDefault(this.props.bootstrapColumnType, "col-sm-3") // tested with 8/3/4
        }
    }

    getWidth() {
        let total = 12.0 // bootstraps total width
        let segments = this.config.bootstrapColumnType.split('-')
        let last = segments[segments.length - 1]
        return Number.parseFloat(last) / total
    }
}

/*class ArticleColumn extends ConfigurableArticleTile {
    constructor(props) {
        super(props)
    }

    render() {
        let articleElements = []
        return e('div', {key: uid++, className: this.config.bootstrapColumnType}, articleElements))
    }
}*/

class ArticleTile extends ConfigurableArticleTile {
    constructor(props) {
        super(props)
    }

    render() {
        let uid = 0

        let rep = this.props.article.representative
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
        if(this.config.showImage) {
            elements.push(e('div', {key: uid++, className: "headlineImage",
                style: {
                    backgroundImage: "url('"+encodeURI(details.image)+"')",
                    backgroundPositionX: x + "%",
                    backgroundPositionY: y + "%"
                }}))
        }

        // Headline
        elements.push(e(this.config.headlineType, {key: uid++}, shorten(details.title, this.config.maxTitleLength)))

        // Description
        elements.push(e('div', {key: uid++, className: "description"}, shorten(details.description, this.config.maxDescriptionLength)))

        // Sources
        elements.push(e('span', {key: uid++, className: "sourceLink"},
            e('span', {key: uid++}, rep.source),
            e('span', {key: uid++}, " +" + (this.props.article.articles.length - 1))
        ))

        return e('div', {key: uid++, className: this.config.bootstrapColumnType}, e('a', {href: rep.url, className: "article"}, elements))
    }
}

class ArticleTileTextAndImageSmall extends ArticleTile {
    constructor(props) {
        super(props)
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
        this.config.bootstrapColumnType = 'col-sm-8'
    }
}

// TODO: Double article
// TODO: Large image article

class ArticleRow extends React.Component {
    constructor(props) {
        super(props)
    }

    render() {
        let types = [ArticleTileTextAndImageSmall, ArticleTileTextSmall, ArticleTileTextLarge]

        let rowWidth = 0

        let articles = this.props.articles
        let columns = []
        let uid = 0
        for(let idx in articles){
            while(true) {
                // TODO this is just for testing
                // choose random type, instantiate to get potential width
                let type = types[Math.floor(Math.random() * types.length)]
                let instance = new type({article: {}})
                let elementWidth = instance.getWidth()

                if(elementWidth < 1.1) {
                    let element = e(type, {key: uid++, article: articles[idx]})
                    columns.push(element)
                    rowWidth += elementWidth
                    break
                }
            }
        }

        return e('div', {className: "row"}, columns)
    }
}

class ArticleGrid extends React.Component {
    constructor(props) {
        super(props)
    }

    render() {
        let articles = this.props.articles

        let rows = []
        let articlesInRow = []
        let uid = 0
        for(let idx in articles){
            articlesInRow.push(articles[idx])
            if(articlesInRow.length >= 4) {
                let element = e(ArticleRow, {key: uid++, articles: articlesInRow})
                rows.push(element)
                articlesInRow = [] // clear
            }
        }

        return e('div', {className: "container"}, rows)
    }
}

const main = async () => {
    const response = await fetch('clusters.json');
    let result = await response.json();
    result = result.reverse()
    //console.log(result) // TODO

    const domContainer = document.querySelector('#articles');
    const root = ReactDOM.createRoot(domContainer);
    root.render(e(ArticleGrid, {articles: result}));
}

main()
