'use strict';

const e = React.createElement;

class ArticleTile extends React.Component {
  constructor(props) {
    super(props);
  }

  render() {
    // Default values
    let maxDescriptionLength = 300
    if(this.props.maxDescriptionLength != undefined)
        maxDescriptionLength = this.props.maxDescriptionLength

    let maxTitleLength = 150
    if(this.props.maxTitleLength != undefined)
        maxTitleLength = this.props.maxTitleLength

    let headlineType = 'h3'
    if(this.props.headlineType != undefined)
        headlineType = this.props.headlineType

    let showImage = true
    if(this.props.showImage != undefined)
        showImage = this.props.showImage

    let bootstrapColumnType = "col-sm-3"
    if(this.props.bootstrapColumnType != undefined)
        bootstrapColumnType = this.props.bootstrapColumnType

    let uid = 0

    let rep = this.props.article.representative
    let details = rep.details
    console.log(rep)

    if(details == null){
        details = {description: "", image: "", imageCenter: {x: 0.5, y: 0.5}}
    }

    let x = (details.imageCenter.x * 100).toFixed()
    let y = (details.imageCenter.y * 100).toFixed()

    let elements = []

    // Image
    if(showImage) {
        elements.push(e('div', {key: uid++, className: "headlineImage",
            style: {
                backgroundImage: "url('"+encodeURI(details.image)+"')",
                backgroundPositionX: x + "%",
                backgroundPositionY: y + "%"
            }}))
    }

    // Headline
    elements.push(e(headlineType, {key: uid++}, shorten(rep.header, maxTitleLength)))

    // Description
    elements.push(e('div', {key: uid++, className: "description"}, shorten(details.description, maxDescriptionLength)))

    // Sources
    elements.push(e('span', {key: uid++, className: "sourceLink"},
        e('span', {key: uid++}, rep.source),
        e('span', {key: uid++}, " +" + (this.props.article.articles.length - 1))
    ))

    return e('div', {key: uid++, className: bootstrapColumnType}, e('a', {href: rep.url, className: "article"}, elements))
  }
}

class ArticleRow extends React.Component {
  constructor(props) {
    super(props);
  }

  render() {
     let articles = this.props.articles
     let columns = []
     let uid = 0
     for(let idx in articles){
         let element = e(ArticleTile, {key: uid++, article: articles[idx]})
         columns.push(element)
     }

    return e('div', {className: "row"}, columns)
  }
}

class ArticleGrid extends React.Component {
  constructor(props) {
    super(props);
  }

  render() {
    let articles = this.props.articles

    let rows = []
    let articlesInRow = []
    let uid = 0
    for(let idx in articles){
        articlesInRow.push(articles[idx])
        if(articlesInRow.length >= 4){
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
