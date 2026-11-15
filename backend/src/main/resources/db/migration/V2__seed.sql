INSERT INTO recycle_category (name, code, unit, sort, status)
VALUES ('废纸', 'PAPER', 'kg', 1, 'ENABLED'),
       ('塑料', 'PLASTIC', 'kg', 2, 'ENABLED'),
       ('金属', 'METAL', 'kg', 3, 'ENABLED'),
       ('旧衣物', 'CLOTH', 'kg', 4, 'ENABLED');

INSERT INTO points_rule (category_id, points_per_kg, effective_from)
SELECT id, 10, '2026-01-01 00:00:00' FROM recycle_category WHERE code = 'PAPER';
INSERT INTO points_rule (category_id, points_per_kg, effective_from)
SELECT id, 15, '2026-01-01 00:00:00' FROM recycle_category WHERE code = 'PLASTIC';
INSERT INTO points_rule (category_id, points_per_kg, effective_from)
SELECT id, 20, '2026-01-01 00:00:00' FROM recycle_category WHERE code = 'METAL';
INSERT INTO points_rule (category_id, points_per_kg, effective_from)
SELECT id, 8, '2026-01-01 00:00:00' FROM recycle_category WHERE code = 'CLOTH';

INSERT INTO site (name, address, open_time, manager, phone, status)
VALUES ('东区回收站', '东区宿舍 1 号楼旁', '08:00-20:00', '李师傅', '13800000000', 'ENABLED');

INSERT INTO product (name, description, points_price, stock, sold_count, spec, exchange_notice, status, version)
VALUES ('洗衣液 500ml', '校园环保款洗衣液', 60, 20, 0, '500ml/瓶', '到回收站出示核销码领取', 'ON', 0),
       ('纸巾一提', '原木抽纸 3 包', 40, 1, 0, '3 包/提', '到回收站出示核销码领取', 'ON', 0),
       ('帆布袋', '环保帆布袋', 80, 0, 0, '标准尺寸', '到回收站出示核销码领取', 'ON', 0),
       ('笔记本', '学生笔记本', 30, 10, 0, 'A5/本', '到回收站出示核销码领取', 'ON', 0),
       ('保温杯', '不锈钢保温杯', 120, 5, 0, '400ml', '到回收站出示核销码领取', 'ON', 0),
       ('旧款台灯', '库存尾货台灯', 90, 3, 0, '单只', '到回收站出示核销码领取', 'OFF', 0);
