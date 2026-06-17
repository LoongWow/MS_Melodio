const fs = require('fs');
let wxss = fs.readFileSync('e:/Desktop/MSMelodio/Melodio-CP/claudeio_frontend/pages/my/my.wxss', 'utf8');

wxss = wxss.replace(/(^|\s|\{|,)\s*view(?=[\s\{,])/g, '$1 div');
wxss = wxss.replace(/(^|\s|\{|,)\s*text(?=[\s\{,])/g, '$1 span');
wxss = wxss.replace(/(^|\s|\{|,)\s*image(?=[\s\{,])/g, '$1 img');
wxss = wxss.replace(/(^|\s|\{|,)\s*scroll-view(?=[\s\{,])/g, '$1 .scroll-view');
wxss = wxss.replace(/(^|\s|\{|,)\s*page(?=[\s\{,])/g, '$1 .page-wrapper');

fs.writeFileSync('e:/Desktop/MSMelodio/Melodio-CP/MS/frontend/src/views/my/My.css', wxss);
