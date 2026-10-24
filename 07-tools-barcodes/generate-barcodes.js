const fs = require('fs');
const { createCanvas } = require('canvas');

const CODE128_PATTERNS = [
    '212222','222122','222221','121223','121322','131222','122213','122312','132212','221213',
    '221312','231212','112232','122132','122231','113222','123122','123221','223211','221132',
    '221231','213212','223112','312131','311222','321122','321221','312212','322112','322211',
    '212123','212321','232121','111323','131123','131321','112313','132113','132311','211313',
    '231113','231311','112133','112331','132131','113123','113321','133121','313121','211331',
    '231131','213113','213311','213131','311123','311321','331121','312113','312311','332111',
    '314111','221411','431111','111224','111422','121124','121421','141122','141221','112214',
    '112412','122114','122411','142112','142211','241211','221114','413111','241112','134111',
    '111242','121142','121241','114212','124112','124211','411212','421112','421211','212141',
    '214121','412121','111424','141124','141421','112414','142114','142411','411412','411214',
    '421114','412114','214141','214112','132132','132321','212131','331111','212122','222211',
    '221133','241111','111431','2331112'
];

const START_B = 104;
const STOP = 106;

function charToCode128B(char) {
    const CHARS = ' !"#$%&\'()*+,-./0123456789:;<=>?@ABCDEFGHIJKLMNOPQRSTUVWXYZ[\\]^_`abcdefghijklmnopqrstuvwxyz{|}~';
    return CHARS.indexOf(char);
}

function calculateChecksum(codes) {
    let checksum = codes[0];
    for (let i = 0; i < codes.length - 1; i++) {
        checksum += codes[i + 1] * (i + 1);
    }
    return checksum % 103;
}

function encode(text) {
    const codes = [START_B];
    for (let i = 0; i < text.length; i++) {
        codes.push(charToCode128B(text[i]));
    }
    const checksum = calculateChecksum(codes);
    codes.push(checksum);
    codes.push(STOP);
    return codes;
}

function drawBarcode(text) {
    const canvas = createCanvas(350, 120);
    const ctx = canvas.getContext('2d');
    const unitWidth = 2;
    const barHeight = 80;
    const startX = 20;
    const startY = 15;

    ctx.fillStyle = '#ffffff';
    ctx.fillRect(0, 0, canvas.width, canvas.height);
    ctx.fillStyle = '#000000';

    const codes = encode(text);
    let currentX = startX;

    codes.forEach(code => {
        const pattern = CODE128_PATTERNS[code];
        if (!pattern) return;
        for (let i = 0; i < pattern.length; i++) {
            const width = parseInt(pattern[i]) * unitWidth;
            if (i % 2 === 0) {
                ctx.fillRect(currentX, startY, width, barHeight);
            }
            currentX += width;
        }
    });

    ctx.fillStyle = '#333333';
    ctx.font = 'bold 16px Arial';
    ctx.textAlign = 'center';
    ctx.fillText(text, canvas.width / 2, barHeight + 35);

    return canvas.toBuffer('image/png');
}

const orderNos = ['ORD260715001', 'ORD260715002', 'ORD260715003', 'ORD260715004', 'ORD260715005'];

orderNos.forEach(orderNo => {
    const buffer = drawBarcode(orderNo);
    fs.writeFileSync(`barcode_${orderNo}.png`, buffer);
    console.log(`Generated: barcode_${orderNo}.png`);
});

console.log('All barcodes generated successfully!');