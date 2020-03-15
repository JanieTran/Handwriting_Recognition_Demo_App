package com.example.android.handwritinrecognition

//-----------------------------------------------------------------------------------
// TESSERACT
//-----------------------------------------------------------------------------------

// Minimum width and height to consider a line
const val WIDTH_THRESHOLD = 400
const val HEIGHT_THRESHOLD = 100

//-----------------------------------------------------------------------------------
// HANDWRITING RECOGNITION MODEL
//-----------------------------------------------------------------------------------

const val INPUT_HEIGHT = 32
const val INPUT_WIDTH = 512
const val INPUT_CHANNEL = 1
const val CHAR_SET = " !\"#&\'()*+,-./0123456789:;?ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz"
const val SEQUENCE_LENGTH = 127

const val INPUT_NODE = "inputs:0"
const val OUTPUT_NODE = "CTCBeamSearchDecoder:1"
const val SEQ_LEN_NODE = "sequence_length:0"