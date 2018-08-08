CREATE VIEW cfuritzau AS
(SELECT * FROM ritzauprogram) UNION (SELECT * FROM mirroredritzauprogram);
