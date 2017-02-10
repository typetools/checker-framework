/*
 * Copyright 2005 Sun Microsystems, Inc.  All Rights Reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Sun designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Sun in the LICENSE file that accompanied this code.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Sun Microsystems, Inc., 4150 Network Circle, Santa Clara,
 * CA 95054 USA or visit www.sun.com if you need additional information or
 * have any questions.
 */

// -- This file was mechanically generated: Do not edit! -- //

/*
 * This class contains a map which records the locale list string for
 * each resource in sun.util.resources & sun.text.resources.
 * It is used to avoid loading non-existent localized resources so that
 * jar files won't be opened unnecessary to look up them.
 *
 * @since 1.6
 */
package sun.util;

import java.util.HashMap;


public class LocaleDataMetaInfo {

    private static final HashMap<String, String> resourceNameToLocales =
        new HashMap<String, String>(6);


    static {
        /* During JDK build time, #XXX_YYY# will be replaced by a string contain all the locales
           supported by the resource.

           Don't remove the space character between " and #. That is put there purposely so that
           look up locale string such as "en" could be based on if it contains " en ".
        */
        resourceNameToLocales.put("sun.text.resources.FormatData",
                                  "  be be_BY bg bg_BG ca ca_ES cs cs_CZ da da_DK de de_AT de_CH de_DE de_LU el el_CY el_GR en en_AU en_CA en_GB en_IE en_IN en_MT en_NZ en_PH en_SG en_US en_ZA es es_AR es_BO es_CL es_CO es_CR es_DO es_EC es_ES es_GT es_HN es_MX es_NI es_PA es_PE es_PR es_PY es_SV es_US es_UY es_VE et et_EE fi fi_FI fr fr_BE fr_CA fr_CH fr_FR fr_LU ga ga_IE hr hr_HR hu hu_HU in in_ID is is_IS it it_CH it_IT lt lt_LT lv lv_LV mk mk_MK ms ms_MY mt mt_MT nl nl_BE nl_NL no no_NO no_NO_NY pl pl_PL pt pt_BR pt_PT ro ro_RO ru ru_RU sk sk_SK sl sl_SI sq sq_AL sr sr_BA sr_CS sr_ME sr_RS sv sv_SE tr tr_TR uk uk_UA |  ar ar_AE ar_BH ar_DZ ar_EG ar_IQ ar_JO ar_KW ar_LB ar_LY ar_MA ar_OM ar_QA ar_SA ar_SD ar_SY ar_TN ar_YE hi_IN iw iw_IL ja ja_JP ja_JP_JP ko ko_KR th th_TH th_TH_TH vi vi_VN zh zh_CN zh_HK zh_SG zh_TW ");

        resourceNameToLocales.put("sun.text.resources.CollationData",
                                  "  be bg ca cs da de el en es et fi fr hr hu is it lt lv mk nl no pl pt ro ru sk sl sq sr sv tr uk |  ar hi iw ja ko th vi zh zh_HK zh_TW ");

        resourceNameToLocales.put("sun.util.resources.TimeZoneNames",
                                  "  de en en_CA en_GB en_IE es fr it sv |  hi ja ko zh_CN zh_HK zh_TW ");

        resourceNameToLocales.put("sun.util.resources.LocaleNames",
                                  "  be bg ca cs da de el el_CY en en_MT en_PH en_SG es es_US et fi fr ga hr hu in is it lt lv mk ms mt nl no pl pt pt_BR pt_PT ro ru sk sl sq sr sv tr uk |  ar hi iw ja ko th vi zh zh_HK zh_SG zh_TW ");

        resourceNameToLocales.put("sun.util.resources.CurrencyNames",
                                  "  be_BY bg_BG ca_ES cs_CZ da_DK de de_AT de_CH de_DE de_GR de_LU el_CY el_GR en_AU en_CA en_GB en_IE en_IN en_MT en_NZ en_PH en_SG en_US en_ZA es es_AR es_BO es_CL es_CO es_CR es_DO es_EC es_ES es_GT es_HN es_MX es_NI es_PA es_PE es_PR es_PY es_SV es_US es_UY es_VE et_EE fi_FI fr fr_BE fr_CA fr_CH fr_FR fr_LU ga_IE hr_HR hu_HU in_ID is_IS it it_CH it_IT lt_LT lv_LV mk_MK ms_MY mt_MT nl_BE nl_NL no_NO pl_PL pt_BR pt_PT ro_RO ru_RU sk_SK sl_SI sq_AL sr_BA sr_CS sr_ME sv sv_SE tr_TR uk_UA |  ar_AE ar_BH ar_DZ ar_EG ar_IQ ar_JO ar_KW ar_LB ar_LY ar_MA ar_OM ar_QA ar_SA ar_SD ar_SY ar_TN ar_YE hi_IN iw_IL ja ja_JP ko ko_KR th_TH vi_VN zh_CN zh_HK zh_SG zh_TW ");

        resourceNameToLocales.put("sun.util.resources.CalendarData",
                                  "  be bg ca cs da de el el_CY en en_GB en_IE en_MT es es_ES es_US et fi fr fr_CA hr hu in_ID is it lt lv mk ms_MY mt mt_MT nl no pl pt pt_PT ro ru sk sl sq sr sv tr uk |  ar hi iw ja ko th vi zh ");
    }

    /*
     * @param resourceName the resource name
     * @return the supported locale string for the passed in resource.
     */
    public static String getSupportedLocaleString(String resourceName) {

        return resourceNameToLocales.get(resourceName);
    }

}
