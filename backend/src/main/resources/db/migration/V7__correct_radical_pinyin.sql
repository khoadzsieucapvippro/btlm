-- Migration: V7__correct_radical_pinyin.sql
-- Description: Correct missing Pinyin values for Kangxi radicals 49 (己 -> jǐ) and 172 (隹 -> zhuī)

UPDATE `RADICAL` SET `pinyin` = 'jǐ' WHERE `radical_id` = 49 AND `character` = '己';
UPDATE `RADICAL` SET `pinyin` = 'zhuī' WHERE `radical_id` = 172 AND `character` = '隹';