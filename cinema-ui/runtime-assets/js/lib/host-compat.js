// HTML helper fallback for DRPY hosts lacking the optional CatVod Function bridge.
// Preserve functions supplied by a site's own bridge.
import {load} from 'assets://js/lib/cheerio.min.js';

function select(html, expression, first) {
    const $ = load(String(html == null ? '' : html));
    let nodes = $.root();
    const steps = String(expression || '').split('&&').filter(Boolean);
    for (const step of steps) {
        nodes = nodes.find(step.trim());
        if (first) nodes = nodes.first();
    }
    return { $, nodes };
}
function pdfh(html, expression) {
    const steps = String(expression || '').split('&&');
    const attribute = steps.pop();
    const { $, nodes } = select(html, steps.join('&&'), true);
    if (!nodes.length) return '';
    if (attribute === 'Text') return nodes.text();
    if (attribute === 'Html') return nodes.html() || '';
    return nodes.attr(attribute) || '';
}
function pdfa(html, expression) {
    if (!html || !String(expression || '').trim()) return [];
    const { $, nodes } = select(html, expression, false);
    return nodes.toArray().map(node => $.html(node));
}
function pdfl(html, expression, textExpression, urlExpression, base) {
    return pdfa(html, expression).map(item =>
        pdfh(item, textExpression) + '$' + pd(item, urlExpression, base));
}
function pd(html, expression, base) {
    const value = pdfh(html, expression).trim();
    if (!value || !base || /^(?:data|javascript|mailto|tel):/i.test(value)) return value;
    return globalThis.joinUrl(String(base), value);
}
const helpers = {pdfh, pdfa, pdfl, pd};
for (const name of Object.keys(helpers)) {
    if (typeof globalThis[name] !== 'function') globalThis[name] = helpers[name];
}
