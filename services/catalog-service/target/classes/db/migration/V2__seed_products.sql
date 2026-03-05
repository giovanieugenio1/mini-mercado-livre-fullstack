-- =============================================================
-- catalog-service — V2: Dados de seed
-- Categorias: ELETRONICOS | VESTUARIO | ESPORTES | CASA | LIVROS | ALIMENTOS
-- imageUrl: caminho local servido pelo Angular (/images/products/*.png)
-- =============================================================

INSERT INTO product (title, description, price, stock, category, image_url, active)
VALUES
    ('iPhone 15 Pro',
     'Smartphone Apple 256 GB, chip A17 Pro, câmera 48 MP, titânio natural.',
     7999.99, 50, 'ELETRONICOS',
     '/images/products/iphone.png', TRUE),

    ('Samsung Galaxy S24',
     'Smartphone Samsung 128 GB, tela AMOLED 6.2", câmera 50 MP.',
     4599.90, 80, 'ELETRONICOS',
     '/images/products/sansung.png', TRUE),

    ('Notebook Dell Inspiron 15',
     'Notebook Intel Core i7 13ª geração, 16 GB RAM, SSD 512 GB.',
     4299.00, 30, 'ELETRONICOS',
     '/images/products/notebook.png', TRUE),

    ('Monitor LG UltraWide 34"',
     'Monitor curvo 34 polegadas, resolução UWQHD, 144 Hz, USB-C.',
     2899.90, 25, 'ELETRONICOS',
     '/images/products/monitor.png', TRUE),

    ('Smart TV 4K 55 Polegadas',
     'Smart TV LED 55", resolução 4K UHD, HDR10, sistema WebOS, Wi-Fi.',
     3199.90, 40, 'ELETRONICOS',
     '/images/products/tv554k.png', TRUE),

    ('Headphone Sony WH-1000XM5',
     'Headphone over-ear com cancelamento de ruído premium, 30h de bateria.',
     2199.00, 40, 'ELETRONICOS',
     '/images/products/headfone.png', TRUE),

    ('Tablet Samsung Galaxy Tab S9',
     'Tablet Android, tela AMOLED 11", 128 GB, S Pen incluída.',
     3199.90, 35, 'ELETRONICOS',
     '/images/products/tab.png', TRUE),

    ('Tênis Nike Air Max 270',
     'Tênis masculino para corrida e uso casual, amortecimento Air Max.',
     599.90, 120, 'ESPORTES',
     '/images/products/tenisnike.png', TRUE),

    ('Camiseta Reserva Classic',
     'Camiseta 100% algodão, gola careca, várias cores disponíveis.',
     129.90, 200, 'VESTUARIO',
     '/images/products/camiseta.png', TRUE),

    ('Air Fryer Philips Walita 4,1L',
     'Fritadeira sem óleo, capacidade 4,1 litros, timer digital, 127V.',
     499.90, 60, 'CASA',
     '/images/products/airfryer.png', TRUE),

    ('Cafeteira Elétrica Nespresso Vertuo',
     'Cafeteira com cápsulas Vertuo, 5 tamanhos de xícara, 1500W.',
     499.00, 45, 'CASA',
     '/images/products/cafeteiraeletrica.png', TRUE),

    ('Livro: Clean Code',
     'Robert C. Martin — O guia definitivo de boas práticas em programação limpa.',
     89.90, 300, 'LIVROS',
     '/images/products/clean.png', TRUE),

    ('Livro: Domain-Driven Design',
     'Eric Evans — Atacando as complexidades no coração do software.',
     99.90, 150, 'LIVROS',
     '/images/products/ddd.png', TRUE)
;
