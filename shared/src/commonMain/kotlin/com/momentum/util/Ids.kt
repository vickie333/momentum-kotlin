package com.momentum.util

import kotlin.uuid.Uuid

fun newId(): String = Uuid.random().toString().uppercase()
