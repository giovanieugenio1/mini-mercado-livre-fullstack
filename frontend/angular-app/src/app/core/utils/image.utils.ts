/**
 * Converte o título do produto em um slug simples para buscar a imagem local.
 *
 * Exemplos:
 *   "Air Fryer"          → "airfryer"
 *   "Smartphone Samsung" → "smartphonesamsungs"
 *   "TV 55'' 4K"         → "tv554k"
 *   "Tênis Nike"         → "tenisnike"
 *   "Cafeteira Elétrica" → "cafeteiraeletrica"
 *
 * A imagem deve ser salva em: public/images/products/{slug}.png
 */
export function productImageSlug(title: string): string {
  return title
    .toLowerCase()
    .normalize('NFD')                    // decompõe caracteres acentuados
    .replace(/[\u0300-\u036f]/g, '')     // remove marcas de acento
    .replace(/[^a-z0-9]/g, '');         // mantém apenas letras e dígitos
}

/**
 * Retorna a URL da imagem do produto.
 * Prioridade:
 *  1. imageUrl vinda da API (se for URL externa completa ou caminho absoluto)
 *  2. Imagem local em /images/products/{slug}.png
 */
export function getProductImageUrl(title: string, apiImageUrl: string | null): string {
  if (apiImageUrl && (apiImageUrl.startsWith('http') || apiImageUrl.startsWith('/'))) {
    return apiImageUrl;
  }
  return `/images/products/${productImageSlug(title)}.png`;
}
